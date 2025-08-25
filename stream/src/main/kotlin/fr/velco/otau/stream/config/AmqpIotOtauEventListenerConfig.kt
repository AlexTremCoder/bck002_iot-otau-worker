package fr.velco.otau.stream.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableConfigurationProperties(RabbitProperties::class)
class AmqpIotOtauEventListenerConfig {
    @Bean("iotOtauEventExchange")
    fun iotOtauEventExchange(rabbitProperties: RabbitProperties): DirectExchange {
        val directExchange = DirectExchange("events")
        directExchange.setAdminsThatShouldDeclare(iotOtauEventAmqpAdmin(rabbitProperties))
        return directExchange
    }

    @Bean("iotOtauEventDlq")
    fun iotOtauEventDlq(rabbitProperties: RabbitProperties): Queue {
        val queue = QueueBuilder.durable("iot-otau-event-dlq").build()
        queue.setAdminsThatShouldDeclare(iotOtauEventAmqpAdmin(rabbitProperties))
        return queue
    }

    @Bean("iotOtauEventConnectionFactory")
    fun iotOtauEventConnectionFactory(rabbitProperties: RabbitProperties): CachingConnectionFactory {
        val connectionFactory = CachingConnectionFactory(rabbitProperties.host, rabbitProperties.port)
        connectionFactory.virtualHost = "applications"
        connectionFactory.username = rabbitProperties.username
        connectionFactory.setPassword(rabbitProperties.password)
        return connectionFactory
    }

    @Bean("iotOtauEventDlqBinding")
    fun iotOtauEventDlqBinding(rabbitProperties: RabbitProperties): Binding {
        val binding = BindingBuilder.bind(iotOtauEventDlq(rabbitProperties)).to(iotOtauEventExchange(rabbitProperties)).with(iotOtauEventDlq(rabbitProperties).name)
        binding.setAdminsThatShouldDeclare(iotOtauEventAmqpAdmin(rabbitProperties))
        return binding
    }

    @Bean("iotOtauEventListenerConnectionFactory")
    fun iotOtauEventListenerConnectionFactory(rabbitProperties: RabbitProperties, retryTemplate: RetryTemplate): SimpleRabbitListenerContainerFactory {
        val configurer = SimpleRabbitListenerContainerFactoryConfigurer(rabbitProperties)

        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

        val factory = SimpleRabbitListenerContainerFactory()
        configurer.configure(factory, iotOtauEventConnectionFactory(rabbitProperties))
        factory.setMessageConverter(Jackson2JsonMessageConverter(objectMapper))
        factory.setErrorHandler(AmqpExceptionHandler())
        factory.setAdviceChain(
            RetryInterceptorBuilder
                .stateless()
                .retryOperations(retryTemplate)
                .recoverer(RejectAndDontRequeueRecoverer())
                .build()
        )
        return factory
    }

    @Bean("iotOtauEventAmqpAdmin")
    fun iotOtauEventAmqpAdmin(rabbitProperties: RabbitProperties): AmqpAdmin {
        val amqpAdmin = RabbitAdmin(iotOtauEventConnectionFactory(rabbitProperties))
        amqpAdmin.afterPropertiesSet()
        return amqpAdmin
    }
}
