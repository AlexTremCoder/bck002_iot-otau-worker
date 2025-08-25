package fr.velco.otau.services.service

import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.dto.rest.DfuDataDto
import fr.velco.otau.services.exception.BadRequestException
import fr.velco.otau.services.exception.DataNotFoundException
import org.springframework.stereotype.Service

/**
 * Manual intervention (Product cache is disable for this service)
 */
@Service
class DfuDataRestService(
    private val dfuDataTopicService: DfuDataTopicService,
    private val productDao: ProductDao, //We directly use ProductDao in place of ProductService to disable cache
) {
    fun treatAskNuotraxVersion(serialNumber: String, dfuDataDto: DfuDataDto) {
        val logCtx = mutableMapOf("context" to "MAINTENANCE", "iotSerialNumber" to serialNumber)
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw DataNotFoundException("Product not found with serialNumber '$serialNumber'")
        this.dfuDataTopicService.sendAskNuotraxVersion(ProductDto.fromEntity(product), logCtx)
    }

    fun treatStartOfTransmission(serialNumber: String, dfuDataDto: DfuDataDto) {
        val logCtx = mutableMapOf("context" to "MAINTENANCE", "iotSerialNumber" to serialNumber)
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw DataNotFoundException("Product not found with serialNumber '$serialNumber'")
        this.dfuDataTopicService.sendStartOfTransmission(
            productDto = ProductDto.fromEntity(product),
            packetCount = dfuDataDto.data?.numberOfLine ?: throw BadRequestException("data.numberOfLine is mandatory"),
            logCtx = logCtx,
        )
    }

    fun treatPacketTransmission(serialNumber: String, dfuDataDto: DfuDataDto) {
        val logCtx = mutableMapOf("context" to "MAINTENANCE", "iotSerialNumber" to serialNumber)
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw DataNotFoundException("Product not found with serialNumber '$serialNumber'")
        val packetNumber: Int = dfuDataDto.data?.packetTransmission?.packetNumber ?: throw BadRequestException("data.packetTransmissionDto.packetNumber is mandatory")
        val packet: String = dfuDataDto.data.packetTransmission.packetData
        if (packet.isEmpty()) throw BadRequestException("data.packetTransmissionDto.packetData cannot be empty")
        this.dfuDataTopicService.sendPacket(ProductDto.fromEntity(product), packetNumber, packet, logCtx)
    }

    fun treatEndOfTransmission(serialNumber: String, dfuDataDto: DfuDataDto) {
        val logCtx = mutableMapOf("context" to "MAINTENANCE", "iotSerialNumber" to serialNumber)
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw DataNotFoundException("Product not found with serialNumber '$serialNumber'")
        this.dfuDataTopicService.sendEndOfTransmission(ProductDto.fromEntity(product), logCtx)
    }

    fun treatAskForLastPacket(serialNumber: String, dfuDataDto: DfuDataDto) {
        val logCtx = mutableMapOf("context" to "MAINTENANCE", "iotSerialNumber" to serialNumber)
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw DataNotFoundException("Product not found with serialNumber '$serialNumber'")
        this.dfuDataTopicService.sendAskForLastPacketId(ProductDto.fromEntity(product), logCtx)
    }

    fun treatUpdateValidation(serialNumber: String, dfuDataDto: DfuDataDto) {
        val logCtx = mutableMapOf("context" to "MAINTENANCE", "iotSerialNumber" to serialNumber)
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw DataNotFoundException("Product not found with serialNumber '$serialNumber'")
        val updateAction = dfuDataDto.data?.updateAction ?: throw BadRequestException("data.updateAction is mandatory")
        this.dfuDataTopicService.sendUpdateValidation(ProductDto.fromEntity(product), updateAction == 1, logCtx)
    }
}
