package fr.velco.otau.services.service

import fr.velco.back.framework.byte.ByteUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.cache.FirmwareCacheService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Handle Dfu Status>Nuotrax version message
 * https://velco-tech.atlassian.net/wiki/spaces/05IT/pages/2515795982/iot-otau-worker+handling+messages#Data-Type-0-%3A-NUOTRAX_VERSION
 * https://velco-tech.atlassian.net/wiki/spaces/RD/pages/1636990985/9.+Topic+-+DFU+STATUS
 *
 * This service begins the OTAU process if requested
 */
@Service
class DataType0NuotraxVersionService(
    private val productDao: ProductDao,
    private val firmwareCacheService: FirmwareCacheService,
    private val dataTypeService: DataTypeService,
    private val dfuDataTopicService: DfuDataTopicService,
    private val otauTrackingService: OtauTrackingService,
    private val productOtauService: ProductOtauService,
    private val logbookService: LogbookService,
) {
    private val log = VelcoLogger {}

    /**
     * BFULL-951
     * The transactional is important to protect over thread concurrency writing
     * Sometimes, the IoTs send many times the same message of his new version after the OTAU: This method is called twice and create duplicate records in db
     */
    @Transactional(rollbackOn = [Throwable::class])
    fun treat(productDto: ProductDto, payload: ByteArray) {
        val logCtx = mutableMapOf("context" to "DFU Status 0:Nuotrax Version", "iotSerialNumber" to productDto.serialNumber)
        log.debug("treat()", logCtx)

        if (payload.size < 9) {
            log.error("Invalid payload (Excepted: 9 but ${payload.size})", logCtx)
            return
        }

        //Update IoT Current versions in DB (firmware version & bootloader version)
        val product = productDao.getReferenceById(productDto.id)
        product.nuotraxFirmwareVersion = this.parseVersion(payload.copyOfRange(1, 5))
        product.bootloaderVersion = this.parseVersion(payload.copyOfRange(5, 9))
        product.lastUpdate = LocalDateTime.now()
        this.productDao.save(product)

        logCtx += mapOf("currentFirmwareVersion" to (product.nuotraxFirmwareVersion ?: ""), "currentBootloaderVersion" to (product.bootloaderVersion ?: ""))
        log.info("Product.nuotraxFirmwareVersion & Product.bootloaderVersion has been updated", logCtx)

        if (!this.dataTypeService.isEligibleToATargetVersion(productDto, logCtx)) {
            log.debug("IoT not eligible to a target version, stop here", logCtx)
            return
        }

        //End of OTAU process
        //After applying a firmware, the IoT reset then send his new version
        //Is the IoT up-to-date?
        val nuotraxFirmware = firmwareCacheService.getFirmware(product.idNuotraxFirmwareAvailable ?: throw Exception("Product.idNuotraxFirmwareAvailable cannot be null here"))
        val targetVersion = nuotraxFirmware.version
        if (product.nuotraxFirmwareVersion.equals(targetVersion, ignoreCase = true)) { //Current version equals Target version?
            log.info("IoT is up-to-date. Now clearing DB Target version", logCtx)
            product.idNuotraxFirmwareAvailable = null //Clear Target version in DB
            product.lastUpdate = LocalDateTime.now()
            this.productDao.save(product)

            //In case of thread concurrency writing here, other threads could throw this exception "Deadlock found when trying to get lock; try restarting transaction"
            this.productOtauService.updateProductOtau(product)
            this.otauTrackingService.stop(productDto, logCtx) //OTAU finish on success
            this.logbookService.sendIotOtauPerformedEvent(productDto)
            return
        }

        //Try to run a new OTAU
        //First, we request the IoT to check his state
        //Either he is ready for a new OTAU: it will answer 2:DFU Packet Data ID/IDLE #0
        //Either he is already in OTAU mode: it will answer 2:DFU Packet Data ID/DFU_RX #n

        if (!this.dataTypeService.isBatteryLevelSufficient(product.batteryLevel, logCtx)) {
            return
        }

        if (!this.otauTrackingService.isOtauSlotAvailable(logCtx)) {
            return
        }

        this.dfuDataTopicService.sendAskForLastPacketId(productDto, logCtx)
    }

    fun parseVersion(payload: ByteArray): String {
        val b0 = ByteUtils.toUnsignedByteAsInt(payload[0])
        val b1 = ByteUtils.toUnsignedByteAsInt(payload[1])
        val b2 = ByteUtils.toUnsignedByteAsInt(payload[2])
        val b3 = ByteUtils.toUnsignedByteAsInt(payload[3])
        return "$b0.$b1.$b2.$b3"
    }
}
