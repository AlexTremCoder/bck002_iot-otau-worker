package fr.velco.otau.services.service

import fr.velco.back.framework.byte.ByteUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.config.Properties
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.enums.FailureReasonEnum
import org.springframework.stereotype.Service

/**
 * Handle Dfu Status>DFU Data Ack / Nack message
 * https://velco-tech.atlassian.net/wiki/spaces/05IT/pages/2515795982/iot-otau-worker+handling+messages#Data-Type-1-%3A-DFU_DATA_ACK_NACK
 * https://velco-tech.atlassian.net/wiki/spaces/RD/pages/1636990985/9.+Topic+-+DFU+STATUS
 *
 * This service handles the packets transfer between IoT <-> SI
 */
@Service
class DataType1DfuDataAckNackService(
    private val properties: Properties,
    private val dataTypeService: DataTypeService,
    private val dfuDataTopicService: DfuDataTopicService,
    private val otauTrackingService: OtauTrackingService,
) {
    private val log = VelcoLogger {}

    fun treat(productDto: ProductDto, payload: ByteArray) {
        val logCtx = mutableMapOf("context" to "DFU Status 1:DFU Data Ack/Nack", "iotSerialNumber" to productDto.serialNumber)
        log.debug("treat()", logCtx)

        if (payload.size != 4) {
            log.error("Invalid payload (Excepted: 4 but ${payload.size})", logCtx)
            return
        }

        val errorMask = payload[1].toInt()
        val ack: Boolean = (errorMask == 0)

        val iotLastAcked: Int = ByteUtils.byteArrayToShort(
            payload.copyOfRange(2, 4) //Bytes 2-3 are the packet number
        ).toInt()

        logCtx += mapOf("iotLastAcked" to iotLastAcked.toString(), "errorMask" to errorMask.toString())

        if (this.dataTypeService.sendEndOfTransmissionIfNotEligibleToATargetVersion(productDto, logCtx)) return
        val otauTracking = this.dataTypeService.getOtauTrackingOrSendEndOfTransmission(productDto, logCtx) ?: return

        //Case of ACK/NACK #1
        //After sending first packet, otau_tracking.last_packet_acked is still NULL in database
        val otauTrackingLastPacketAcked = otauTracking.lastPacketAcked ?: 0

        if (ack) { //ACK case
            if (iotLastAcked != (otauTrackingLastPacketAcked + 1)) { //Unexpected case
                //Ignore the message if
                //- IoT ack an old packet already acked
                //- IoT ack again the same packet (To avoid ping-ping between SI <-> IoT)
                //- IoT ack a future packet (Theoretically impossible, we never have sent this packet)
                if (otauTracking.lastPacketAcked != null) logCtx += mapOf("lastPacketAcked" to otauTrackingLastPacketAcked.toString())
                log.info("Receive ACK with unexpected index", logCtx) //Maybe remove this log if occur too frequently
                return
            }

            if ((iotLastAcked % properties.logModulo) == 0) log.info("Receive ACK", logCtx)

            this.otauTrackingService.setLastPacketAcked(productDto, iotLastAcked, logCtx)

            logCtx += mapOf("lastPacketAcked" to iotLastAcked.toString())

            if (this.dataTypeService.isLastPacket(productDto, iotLastAcked, logCtx)) { //All packets transmitted?
                log.info("Receive last packet ACK", logCtx)
                this.dfuDataTopicService.sendEndOfTransmission(productDto, logCtx)
                return
            }

            val packetToSend = iotLastAcked + 1
            logCtx += mapOf("packetToSend" to packetToSend.toString())
            this.dataTypeService.sendPacket(productDto, packetToSend, logCtx)

        } else { //NACK cases
            if (iotLastAcked != otauTrackingLastPacketAcked) { //Unexpected case
                //Ignore the message if
                //- IoT nack an old packet already acked
                //- IoT nack a future packet (Theoretically impossible, we never have sent this packet)
                if (otauTracking.lastPacketAcked != null) logCtx += mapOf("lastPacketAcked" to otauTrackingLastPacketAcked.toString())
                log.info("Receive NACK with unexpected index", logCtx) //Maybe remove this log if occur too frequently
                return
            }

            val nackPacketCounterConsecutive = this.otauTrackingService.incrementNack(productDto, logCtx)

            logCtx += mapOf("lastPacketAcked" to iotLastAcked.toString(), "nackPacketCounterConsecutive" to nackPacketCounterConsecutive.toString())
            log.info("Receive NACK", logCtx)
            if (nackPacketCounterConsecutive >= properties.maxSendPacketAttempts) {
                log.error("Reach max NACK packet error counter. Stop OTAU", logCtx)
                this.otauTrackingService.stop(productDto, logCtx, FailureReasonEnum.TOO_MANY_NACK)
                this.dfuDataTopicService.sendEndOfTransmission(productDto, logCtx)
            }
        }
    }
}
