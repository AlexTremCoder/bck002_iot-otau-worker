package fr.velco.otau.services.service

import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.service.cache.ProductCacheService
import org.springframework.stereotype.Service

@Service
class TriggerService(
    private val productCacheService: ProductCacheService,
    private val otauTrackingService: OtauTrackingService,
    private val dfuDataTopicService: DfuDataTopicService,
) {
    private val log = VelcoLogger {}

    /**
     * Go/no go for OTAU
     */
    fun treat(serialNumber: String) {
        val logCtx = mutableMapOf("iotSerialNumber" to serialNumber)
        log.debug("TriggerService.treat()", logCtx)

        val productDto = productCacheService.getProduct(serialNumber)
        if (productDto.idFirmware != null) { //is OTAU scheduled?
            if (otauTrackingService.isOtauSlotAvailable(logCtx)) {            
            	dfuDataTopicService.sendAskNuotraxVersion(productDto, logCtx)
            }
        }
    }
}
