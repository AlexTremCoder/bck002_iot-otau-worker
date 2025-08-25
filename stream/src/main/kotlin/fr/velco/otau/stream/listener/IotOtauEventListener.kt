package fr.velco.otau.stream.listener

import fr.velco.otau.stream.dto.IotOtauEventMessage
import fr.velco.otau.services.service.TriggerService
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.*
import org.springframework.stereotype.Component

@Component
class IotOtauEventListener(
    private val triggerService: TriggerService,
) {
    @RabbitListener(
        containerFactory = "iotOtauEventListenerConnectionFactory",
        bindings = [QueueBinding(
            value = Queue(
                value = "iot-otau-event",
                durable = "true",
                admins = ["iotOtauEventAmqpAdmin"],
                arguments = [
                    Argument(
                        name = "x-dead-letter-exchange",
                        value = "events",
                    ),
                    Argument(
                        name = "x-dead-letter-routing-key",
                        value = "iot-otau-event-dlq",
                    )],
                ignoreDeclarationExceptions = "true"
            ),
            exchange = Exchange(
                value = "events",
                type = ExchangeTypes.DIRECT,
                ignoreDeclarationExceptions = "true",
                admins = ["iotOtauEventAmqpAdmin"],
            ),
            key = ["iot-otau-event"],
            admins = ["iotOtauEventAmqpAdmin"],
        )], concurrency = "\${spring.rabbitmq.iot-otau-event-consumers}"
    )
    fun iotOtauEventListener(iotOtauEventMessage: IotOtauEventMessage) { //Message example: {"iotSerialNumber": "N155AD"}
        this.triggerService.treat(iotOtauEventMessage.iotSerialNumber) //Launch OTAU?
    }
}
