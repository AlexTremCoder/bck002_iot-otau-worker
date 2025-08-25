package fr.velco.otau.stream.listener

import fr.velco.back.framework.logging.VelcoLogger
import fr.velco.otau.services.service.DfuStatusTopicService
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.*
import org.springframework.stereotype.Component

@Component
class NuotraxTopicListener(
    private val dfuStatusTopicService: DfuStatusTopicService,
) {
    private val log = VelcoLogger {}

    @RabbitListener(
        containerFactory = "nuotraxTopicListenerConnectionFactory",
        bindings = [QueueBinding(
            value = Queue(
                value = "otau",
                durable = "true",
                admins = ["nuotraxTopicAmqpAdmin"],
                arguments = [
                    Argument(
                        name = "x-dead-letter-exchange",
                        value = "amq.topic",
                    ),
                    Argument(
                        name = "x-dead-letter-routing-key",
                        value = "otau-dlq",
                    )],
                ignoreDeclarationExceptions = "true"
            ),
            exchange = Exchange(
                value = "amq.topic",
                type = ExchangeTypes.TOPIC,
                ignoreDeclarationExceptions = "true",
                admins = ["nuotraxTopicAmqpAdmin"],
            ),
            key = ["*.*.u"],
            admins = ["nuotraxTopicAmqpAdmin"],
        )], concurrency = "\${spring.rabbitmq.otau-topic-consumers}"
    )
    fun listener(message: Message) {
        val routingKey = message.messageProperties.receivedRoutingKey

        log.debug("Payload: ${message.body} for routing key: '$routingKey'")

        try {
            dfuStatusTopicService.treat(serialNumber = getSerialNumber(routingKey), encryptedPayload = message.body)
        } catch (e: Exception) {
            log.error(e.message ?: "", mapOf("iotSerialNumber" to getSerialNumber(routingKey)))
        }
    }

    /**
     * Extract IoT serial number
     * E.g. M.M1D721.s -> M1D721
     */
    private fun getSerialNumber(routingKey: String): String = routingKey.substring(2, routingKey.length - 2)
}
