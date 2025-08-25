package fr.velco.otau.services.service

import fr.velco.back.framework.amqp.VelcoAmqpPublisher
import fr.velco.otau.services.dto.ProductDto
import fr.velco.otau.services.dto.logbook.LogbookEventAttributeDto
import fr.velco.otau.services.dto.logbook.LogbookEventDto
import fr.velco.otau.services.enums.logbook.EventDetailEnum
import fr.velco.otau.services.enums.logbook.EventTypeEnum
import fr.velco.otau.services.enums.logbook.RelativeEntityTypeEnum
import org.springframework.stereotype.Service

@Service
class LogbookService(
    private val velcoAmqpPublisher: VelcoAmqpPublisher,
) {
    fun sendIotOtauPerformedEvent(productDto: ProductDto) {
        val attributes = listOf(LogbookEventAttributeDto("peripheralSn", productDto.serialNumber))
        velcoAmqpPublisher.publishLogbookEvent(
            LogbookEventDto(
                RelativeEntityTypeEnum.PRODUCT.name,
                productDto.id,
                EventTypeEnum.IOT_OTAU.name,
                EventDetailEnum.IOT_OTAU_PERFORMED.name,
                attributes,
                null,
            )
        )
    }
}
