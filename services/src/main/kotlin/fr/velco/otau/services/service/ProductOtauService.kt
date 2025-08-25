package fr.velco.otau.services.service

import fr.velco.otau.persistences.velco.dao.ProductOtauDao
import fr.velco.otau.persistences.velco.table.Product
import fr.velco.otau.persistences.velco.table.ProductOtau
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProductOtauService(
    private val productOtauDao: ProductOtauDao,
) {
    fun updateProductOtau(product: Product) {
        val productOtau = this.productOtauDao.findFirstByProductId(product.id) ?: ProductOtau(product = product)
        productOtau.iotFwUpdateDate = LocalDateTime.now()
        this.productOtauDao.save(productOtau)
    }
}
