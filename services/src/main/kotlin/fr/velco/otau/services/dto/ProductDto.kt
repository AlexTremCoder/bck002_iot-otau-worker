package fr.velco.otau.services.dto

import fr.velco.otau.persistences.velco.table.Product

class ProductDto(
    val id: Long,
    val aesKey: ByteArray,
    val serialNumber: String,
    val idFirmware: Long?,
) {
    companion object {
        fun fromEntity(product: Product) = ProductDto(
            id = product.id,
            aesKey = product.aesKey ?: throw Exception("This product has no aesKey"),
            serialNumber = product.serialNumber,
            idFirmware = product.idNuotraxFirmwareAvailable,
        )
    }
}
