package fr.velco.otau.services.config

import org.cache2k.extra.spring.SpringCache2kCacheManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig(
    @Value("\${cache-configurations.caches.product.ttl-in-seconds:1800}") private val productCacheExpirationInSeconds: Long,
    @Value("\${cache-configurations.caches.firmware.ttl-in-seconds:86400}") private val firmwareCacheExpirationInSeconds: Long,
) {
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SpringCache2kCacheManager()
        cacheManager.addCache { it.name(PRODUCT_CACHE).expireAfterWrite(productCacheExpirationInSeconds, TimeUnit.SECONDS) }
        cacheManager.addCache { it.name(FIRMWARE_CACHE).expireAfterWrite(firmwareCacheExpirationInSeconds, TimeUnit.SECONDS) }
        return cacheManager
    }

    companion object {
        const val FIRMWARE_CACHE = "firmware"
        const val PRODUCT_CACHE = "product"
    }
}
