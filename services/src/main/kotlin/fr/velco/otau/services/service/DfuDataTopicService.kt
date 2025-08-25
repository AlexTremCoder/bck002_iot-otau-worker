package fr.velco.otau.services.service

import fr.velco.back.framework.aes.AesUtils
import fr.velco.back.framework.byte.ByteUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.port.MqttPort
import fr.velco.back.framework.byte.HexStringUtils
import org.springframework.stereotype.Service
import java.util.*

/**
 * Messages SI -> IoT (See DfuStatusTopicService for IoT responses)
 * https://velco-tech.atlassian.net/wiki/spaces/RD/pages/1636761619/8.+Topic+-+DFU+DATA
 */
@Service
class DfuDataTopicService(
    private val mqttPort: MqttPort,
) {
    private val log = VelcoLogger {}

    fun sendAskNuotraxVersion(productDto: ProductDto, logCtx: MutableMap<String, String>) {
        log.info("Send 'Ask for Nuotrax Version'", logCtx)

        val payload = ByteArray(1)
        payload[0] = 0x00 //Ask for Nuotrax Version
        cryptAndSendPayload(productDto, payload, logCtx)
    }

    fun sendStartOfTransmission(productDto: ProductDto, packetCount: Int, logCtx: MutableMap<String, String>) {
        log.info("Send 'Start of Transmission'", logCtx)

        val packetCountByteArray: ByteArray = ByteUtils.shortToByteArray(packetCount.toShort())
        val payload = ByteArray(4)
        payload[0] = 0x01 //Start of Transmission
        payload[1] = packetCountByteArray[0]
        payload[2] = packetCountByteArray[1]
        payload[3] = 0x00 //mqtt mode
        cryptAndSendPayload(productDto, payload, logCtx)
    }

    fun sendEndOfTransmission(productDto: ProductDto, logCtx: MutableMap<String, String>) {
        log.info("Send 'End of Transmission'", logCtx)

        val payload = ByteArray(1)
        payload[0] = 0x03 //End of Transmission
        cryptAndSendPayload(productDto, payload, logCtx)
    }

    fun sendPacket(productDto: ProductDto, packetNumber: Int, packet: String, logCtx: MutableMap<String, String>) {
        log.debug("sendPacket(packetNumber: $packetNumber)", logCtx)

        val byteArray: ByteArray = Base64.getDecoder().decode(packet)

        val packetNumberByteArray: ByteArray = ByteUtils.shortToByteArray(packetNumber.toShort())
        val payload = ByteArray(byteArray.size + 3)
        payload[0] = 0x02 //Packet Transmission
        payload[1] = packetNumberByteArray[0]
        payload[2] = packetNumberByteArray[1]
        byteArray.copyInto(payload, 3, 0, byteArray.size) //Include address & checksum
        cryptAndSendPayload(productDto, payload, logCtx)
    }

    fun sendUpdateValidation(productDto: ProductDto, validation: Boolean, logCtx: MutableMap<String, String>) {
        val logText: String
        val validationByte: Byte

        if (validation) {
            logText = "Send 'Update Validation/DFU Validation'"
            validationByte = 0x01
        } else {
            logText = "Send 'Update Validation/DFU Cancel'"
            validationByte = 0x02
        }

        log.info(logText, logCtx)

        val payload = ByteArray(4)
        payload[0] = 0x05 //Update Validation
        payload[1] = validationByte //01: DFU Validation 02: DFU Cancel
        cryptAndSendPayload(productDto, payload, logCtx)
    }

    fun sendAskForLastPacketId(productDto: ProductDto, logCtx: MutableMap<String, String>) {
        log.info("Send 'Ask for Last Packet Transmit'", logCtx)

        val payload = ByteArray(1)
        payload[0] = 0x04 //Ask for Last Packet Transmit
        cryptAndSendPayload(productDto, payload, logCtx)
    }

    fun cryptAndSendPayload(productDto: ProductDto, payload: ByteArray, logCtx: MutableMap<String, String>) {
        val topic = productDto.serialNumber.substring(0, 1).uppercase(Locale.getDefault()) + "/" + productDto.serialNumber + "/d"
        log.debug("cryptAndSendPayload(${HexStringUtils.toHexString(payload)} to topic $topic", logCtx)

        val encryptedPayload = AesUtils.encrypt(productDto.aesKey, payload)
        this.mqttPort.send(topic, encryptedPayload)
    }
}
