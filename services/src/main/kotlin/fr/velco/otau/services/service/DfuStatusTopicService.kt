package fr.velco.otau.services.service

import fr.velco.back.framework.aes.AesUtils
import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.enums.DfuStatusDataTypeEnum
import fr.velco.otau.services.service.cache.ProductCacheService
import org.springframework.stereotype.Service

/**
 * Messages IoT -> SI (See DfuDataTopicService for SI responses)
 * https://velco-tech.atlassian.net/wiki/spaces/RD/pages/1636990985/9.+Topic+-+DFU+STATUS
 */
@Service
class DfuStatusTopicService(
    private val productCacheService: ProductCacheService,
    private val dataType0NuotraxVersionService: DataType0NuotraxVersionService,
    private val dataType1DfuDataAckNackService: DataType1DfuDataAckNackService,
    private val dataType2DfuPacketDataIdService: DataType2DfuPacketDataIdService,
    private val dataType3DfuReadyStatusService: DataType3DfuReadyStatusService,
) {
    private val log = VelcoLogger {}

    fun treat(serialNumber: String, encryptedPayload: ByteArray) {
        val logCtx = mapOf("iotSerialNumber" to serialNumber)
        log.debug("treat()", logCtx)

        try {
            val productDto = this.productCacheService.getProduct(serialNumber)
            val payload = AesUtils.decrypt(productDto.aesKey, encryptedPayload)
            if (payload.isEmpty()) throw Exception("Payload of a DFU STATUS message cannot be empty")

            val dfuStatusDataTypeByte = payload[0] //First byte is the "Data Type Value"
            val type: DfuStatusDataTypeEnum = DfuStatusDataTypeEnum.from(dfuStatusDataTypeByte.toInt()) ?: throw Exception("Unknown DFU status data type ($dfuStatusDataTypeByte)")

            log.debug("type=$type", logCtx)
            when (type) {
                DfuStatusDataTypeEnum.NUOTRAX_VERSION -> this.dataType0NuotraxVersionService.treat(productDto, payload)
                DfuStatusDataTypeEnum.DFU_DATA_ACK_NACK -> this.dataType1DfuDataAckNackService.treat(productDto, payload)
                DfuStatusDataTypeEnum.DFU_PACKET_DATA_ID -> this.dataType2DfuPacketDataIdService.treat(productDto, payload)
                DfuStatusDataTypeEnum.DFU_READY_STATUS -> this.dataType3DfuReadyStatusService.treat(productDto, payload)
            }
        } catch (e: Exception) {
            log.error(e.message ?: "Unexpected error", logCtx)
        }
    }
}
