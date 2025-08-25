package fr.velco.otau.services.service.cache

import fr.velco.otau.persistences.velco.dao.ProductDao
import fr.velco.otau.services.config.CacheConfig.Companion.PRODUCT_CACHE
import fr.velco.otau.services.dto.ProductDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Provide a cache to avoid overloading unnecessarily database
 */
@Service
class ProductCacheService(
    private val productDao: ProductDao,
) {
    @Cacheable(PRODUCT_CACHE) //See CacheConfig for expiration time
    fun getProduct(serialNumber: String): ProductDto {
        val product = productDao.findFirstBySerialNumber(serialNumber) ?: throw Exception("Product not found in velco.Product for serialNumber '$serialNumber'")
        return ProductDto.fromEntity(product)
    }
}
