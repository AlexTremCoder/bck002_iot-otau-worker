package fr.velco.otau.services.service

import fr.velco.back.framework.byte.ByteUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.enums.FailureReasonEnum
import org.springframework.stereotype.Service

/**
 * Handle Dfu Status>DFU Ready status
 * https://velco-tech.atlassian.net/wiki/spaces/05IT/pages/2515795982/iot-otau-worker+handling+messages#Data-Type-3-%3A-DFU_READY_STATUS
 * https://velco-tech.atlassian.net/wiki/spaces/RD/pages/1636990985/9.+Topic+-+DFU+STATUS
 *
 * This service handles the final phase of OTAU (Apply the firmware)
 */
@Service
class DataType3DfuReadyStatusService(
    private val dfuDataTopicService: DfuDataTopicService,
    private val dataTypeService: DataTypeService,
    private val otauTrackingService: OtauTrackingService,
) {
    private val log = VelcoLogger {}

    fun treat(productDto: ProductDto, payload: ByteArray) {
        val logCtx = mutableMapOf("context" to "DFU Status 3:DFU Ready status", "iotSerialNumber" to productDto.serialNumber)
        log.debug("treat()", logCtx)

        if (this.dataTypeService.sendDfuCancelIfNotEligibleToATargetVersion(productDto, logCtx)) return
        if (this.dataTypeService.sendDfuCancelIfNoTracking(productDto, logCtx)) return

        if (payload.size != 2) {
            log.error("Invalid payload (Excepted: 2 but ${payload.size})", logCtx)
            return
        }
        val errorMask = payload[1].toInt()

        if (errorMask == 0) {
            this.dfuDataTopicService.sendUpdateValidation(productDto, true, logCtx) //Go for flashing
            return
        }

        val errorCrcIncoherency: Boolean = ByteUtils.getBit(errorMask, 0)
        val errorKey1Incoherency: Boolean = ByteUtils.getBit(errorMask, 1)
        val errorKey2Incoherency: Boolean = ByteUtils.getBit(errorMask, 2)
        val errorSignatureIncoherency: Boolean = ByteUtils.getBit(errorMask, 3)

        if (errorKey1Incoherency) {
            log.error("Key 1 incoherency, abort OTAU", logCtx)
            this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.KEY1_INCOHERENCY) //Useless to restart the OTAU (The same causes produce the same effects)
            return
        }
        if (errorKey2Incoherency) {
            log.error("Key 2 incoherency, abort OTAU", logCtx)
            this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.KEY2_INCOHERENCY) //Useless to restart the OTAU (The same causes produce the same effects)
            return
        }
        if (errorSignatureIncoherency) {
            log.error("Digital signature incoherency, abort OTAU", logCtx)
            this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.SIGNATURE_INCOHERENCY) //Useless to restart the OTAU (The same causes produce the same effects)
            return
        }

        if (errorCrcIncoherency) { //If this error occurs too frequently, in the future, we could try to restart the OTAU here with this.dfuDataTopicService.sendAskNuotraxVersion()
            log.error("CRC incoherency detected by the IoT, abort OTAU", logCtx)
            this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.CRC_INCOHERENCY)
            return
        }

        //Should not occur
        log.error("Unknown error detected by the IoT, abort OTAU", logCtx)
        this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.UNKNOWN_MASK_DFU_READY_STATUS)
    }
}
