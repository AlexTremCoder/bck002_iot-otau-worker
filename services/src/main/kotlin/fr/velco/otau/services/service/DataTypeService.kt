package fr.velco.otau.services.service

import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.enums.FailureReasonEnum
import fr.velco.otau.persistences.velco.table.OtauTracking
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.service.cache.FirmwareCacheService
import org.springframework.stereotype.Service

/**
 * Common functions for DataType*Service
 */
@Service
class DataTypeService(
    private val properties: Properties,
    private val dfuDataTopicService: DfuDataTopicService,
    private val firmwareCacheService: FirmwareCacheService,
    private val otauTrackingService: OtauTrackingService,
) {
    private val log = VelcoLogger {}

    /**
     * Check if an OTAU bas been requested
     */
    fun isEligibleToATargetVersion(productDto: ProductDto, logCtx: MutableMap<String, String>): Boolean {
        log.debug("isEligibleToATargetVersion()", logCtx)

        if (productDto.idFirmware == null) {
            log.debug("isEligibleToATargetVersion()=false (Product.idNuotraxFirmwareAvailable IS NULL)", logCtx)
            return false
        }
        log.debug("isEligibleToATargetVersion()=true", logCtx)
        return true
    }

    /**
     * Send 'End of transmission' if OTAU has been canceled by admin
     */
    fun sendEndOfTransmissionIfNotEligibleToATargetVersion(productDto: ProductDto, logCtx: MutableMap<String, String>): Boolean {
        log.debug("sendEndOfTransmissionIfNotEligibleToATargetVersion()", logCtx)

        if (this.isEligibleToATargetVersion(productDto, logCtx)) return false

        log.info("Abnormal situation, IoT is not eligible to a Target version. Stop OTAU", logCtx)
        this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.CANCELED)
        this.dfuDataTopicService.sendEndOfTransmission(productDto, logCtx)
        return true
    }

    /**
     * Send 'Update Validation/DFU Cancel' if OTAU has been canceled by admin
     */
    fun sendDfuCancelIfNotEligibleToATargetVersion(productDto: ProductDto, logCtx: MutableMap<String, String>): Boolean {
        log.debug("sendDfuCancelIfNotEligibleToATargetVersion()", logCtx)

        if (this.isEligibleToATargetVersion(productDto, logCtx)) return false

        log.info("Abnormal situation, IoT is not eligible to a Target version. Stop OTAU", logCtx)
        this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.CANCELED)
        this.dfuDataTopicService.sendUpdateValidation(productDto, false, logCtx) //No go for flashing
        return true
    }

    /**
     * Send 'End of transmission' if no tracking found
     * Should never occur (All OTAU are always initialized by this worker with a new record in otau_tracking)
     */
    fun getOtauTrackingOrSendEndOfTransmission(productDto: ProductDto, logCtx: MutableMap<String, String>): OtauTracking? {
        log.debug("getOtauTrackingOrSendEndOfTransmission()", logCtx)

        val otauTracking = this.otauTrackingService.getOtauTracking(productDto, logCtx)
        if (otauTracking == null) {
            this.dfuDataTopicService.sendEndOfTransmission(productDto, logCtx)
        }
        return otauTracking
    }

    /**
     * Send 'End of transmission' if no tracking found
     * Should never occur (All OTAU are always initialized by this worker with a new record in otau_tracking)
     */
    fun sendDfuCancelIfNoTracking(productDto: ProductDto, logCtx: MutableMap<String, String>): Boolean {
        log.debug("sendDfuCancelIfNoTracking()", logCtx)

        val otauTracking = this.otauTrackingService.getOtauTracking(productDto, logCtx)
        if (otauTracking == null) {
            this.dfuDataTopicService.sendUpdateValidation(productDto, false, logCtx) //No go for flashing
            return true
        }
        return false
    }

    fun sendPacket(productDto: ProductDto, packetNumber: Int, logCtx: MutableMap<String, String>, logWithoutModulo: Boolean = false) {
        if (logWithoutModulo || ((packetNumber % properties.logModulo) == 0)) log.info("Send packet", logCtx)

        val firmware = this.firmwareCacheService.getFirmware(productDto.idFirmware ?: throw Exception("Product.idNuotraxFirmwareAvailable cannot be null here"))
        if (packetNumber <= 0) throw Exception("packet number must be greater than 0 (packetNumber: $packetNumber)")
        if (packetNumber > firmware.totalPackets) throw Exception("packet number out of bounds (packetNumber: $packetNumber max: ${firmware.totalPackets}")

        val packet: String = firmware.jsonArray.getString(packetNumber - 1)
        this.dfuDataTopicService.sendPacket(productDto, packetNumber, packet, logCtx)
    }

    fun isLastPacket(productDto: ProductDto, packetNumber: Int, logCtx: MutableMap<String, String>): Boolean {
        log.debug("isLastPacket()", logCtx)
        val firmware = this.firmwareCacheService.getFirmware(productDto.idFirmware ?: throw Exception("Product.idNuotraxFirmwareAvailable cannot be null here"))
        return (packetNumber >= firmware.totalPackets)
    }
}
