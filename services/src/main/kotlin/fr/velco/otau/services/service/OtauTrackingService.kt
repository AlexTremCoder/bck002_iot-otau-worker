package fr.velco.otau.services.service

import fr.velco.back.framework.logging.VelcoLogger

import fr.velco.otau.services.enums.FailureReasonEnum
import fr.velco.otau.persistences.velco.dao.OtauTrackingDao
import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.persistences.velco.table.OtauTracking
import fr.velco.otau.persistences.velco.table.Product
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

/**
 * Manage the velco.otau_tracking table
 */
@Service
class OtauTrackingService(
    private val properties: Properties,
    private val productDao: ProductDao,
    private val otauTrackingDao: OtauTrackingDao,
    private val otauHistoryService: OtauHistoryService,
    private val firmwareCacheService: FirmwareCacheService,
) {
    private val log = VelcoLogger {}

    fun start(product: Product, logCtx: MutableMap<String, String>) {
        log.debug("start()", logCtx)

        val otauTracking = this.otauTrackingDao.findFirstByIdProduct(product.id)
        if (otauTracking != null) { //A previous OTAU not completed exists ?
            log.warn("Start a new OTAU but an old one already exists in otau_tracking table. Close it", logCtx)
            this.stop(ProductDto.fromEntity(product), logCtx, FailureReasonEnum.NEW_OTAU_BEFORE_END_OF_PREVIOUS)
        }

        val firmware = this.firmwareCacheService.getFirmware(product.idNuotraxFirmwareAvailable ?: throw Exception("Product.idNuotraxFirmwareAvailable cannot be null here"))
        this.otauTrackingDao.save(
            OtauTracking(
                idProduct = product.id,
                currentFirmwareVersion = product.nuotraxFirmwareVersion ?: "",
                currentBootloaderVersion = product.bootloaderVersion ?: "",
                targetFirmwareVersion = firmware.version,
                durationInMinutes = 0,
                totalPacketsToSend = firmware.totalPackets,
                nackPacketCounterConsecutive = 0,
                nackPacketCounterTotal = 0,
                progressPercentage = 0,
            )
        )
    }

    fun stop(productDto: ProductDto, logCtx: MutableMap<String, String>, failureReasonEnum: FailureReasonEnum? = null) {
        log.debug("stop()", logCtx)

        val otauTracking = this.getOtauTracking(productDto, logCtx) ?: return

        //Compute duration
        val now = LocalDateTime.now()
        val duration = Duration.between(otauTracking.startDate, now)
        otauTracking.durationInMinutes = duration.toMinutes().toInt()

        this.otauHistoryService.add(otauTracking, failureReasonEnum)
        this.otauTrackingDao.delete(otauTracking) //Remove it
    }

    fun incrementNack(productDto: ProductDto, logCtx: MutableMap<String, String>): Int {
        log.debug("incrementNack()", logCtx)

        val otauTracking = this.getOtauTracking(productDto, logCtx) ?: return 0

        otauTracking.nackPacketCounterConsecutive++
        otauTracking.nackPacketCounterTotal++
        otauTracking.lastUpdate = LocalDateTime.now()
        this.otauTrackingDao.save(otauTracking)

        return otauTracking.nackPacketCounterConsecutive
    }

    /**
     * Return false if no OTAU in progress
     */
    fun setLastPacketAcked(productDto: ProductDto, lastPacketAcked: Int, logCtx: MutableMap<String, String>): Boolean {
        log.debug("setLastPacketAcked()", logCtx)

        val otauTracking = this.getOtauTracking(productDto, logCtx) ?: return false

        //Compute progressPercent
        otauTracking.progressPercentage = ((lastPacketAcked.toFloat() / otauTracking.totalPacketsToSend.toFloat()) * 100).toInt().toShort()
        otauTracking.nackPacketCounterConsecutive = 0
        otauTracking.lastPacketAcked = lastPacketAcked
        otauTracking.lastUpdate = LocalDateTime.now()
        this.otauTrackingDao.save(otauTracking)
        return true
    }

    /**
     * When this method is called, a record must exist into otau_tracking
     * If not, log an error message
     */
    fun getOtauTracking(productDto: ProductDto, logCtx: MutableMap<String, String>): OtauTracking? {
        val otauTracking = this.otauTrackingDao.findFirstByIdProduct(productDto.id)
        if (otauTracking == null) {
            log.error("Abnormal situation, there is no known OTAU in progress. (Tracking & history are impossible)", logCtx)
        }
        return otauTracking
    }

    /**
     * Stop obsolete OTAU and move them to otau_history
     * @Return The number of "active" OTAU
     */
    fun cleanupAndReturnNumberOfActiveOtau(): Int {
        log.debug("cleanupAndReturnNumberOfActiveOtau()")

        val now = LocalDateTime.now()
        val activeDateLimit = now.minusMinutes(properties.activeDelayInMinutes)
        val obsoleteDateLimit = now.minusDays(properties.obsoleteDelayInDays)

        val numberOfActive = otauTrackingDao.countByLastUpdateAfter(activeDateLimit)
        val obsoleteOtauTrackingList = otauTrackingDao.findByLastUpdateBefore(obsoleteDateLimit)

        log.debug("Number of active OTAU $numberOfActive. Number of obsolete OTAU ${obsoleteOtauTrackingList.size}")

        //Move obsolete otau_tracking to otau_history
        obsoleteOtauTrackingList.forEach {
            val product = productDao.getReferenceById(it.idProduct)
            log.info("STALL_FOR_TOO_LONG detected, move it to otau_history", mapOf("iotSerialNumber" to product.serialNumber))
            this.stop(ProductDto.fromEntity(product), mutableMapOf(), FailureReasonEnum.STALL_FOR_TOO_LONG)
        }

        return numberOfActive
    }

    /**
     * Check if a slot is available to start a new OTAU.
     * The number of active OTAU is added into the log context.
     */
    fun isOtauSlotAvailable(logCtx: MutableMap<String, String>): Boolean {
        val numberOfActiveOtau = this.cleanupAndReturnNumberOfActiveOtau()
        logCtx += mapOf("numberOfActiveOtau" to numberOfActiveOtau.toString())
        val slotAvailable = numberOfActiveOtau < properties.maxSimultaneousOtau
        if (!slotAvailable) {
            log.info("Max active OTAU reached", logCtx)
        }
        return slotAvailable
    }
}
