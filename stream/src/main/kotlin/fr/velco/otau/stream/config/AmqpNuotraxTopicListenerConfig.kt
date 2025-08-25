package fr.velco.otau.stream.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableConfigurationProperties(RabbitProperties::class)
class AmqpNuotraxTopicListenerConfig {
    @Bean("nuotraxTopicExchange")
    fun nuotraxTopicExchange(rabbitProperties: RabbitProperties): TopicExchange {
        val topicExchange = TopicExchange("amq.topic")
        topicExchange.setAdminsThatShouldDeclare(nuotraxTopicAmqpAdmin(rabbitProperties))
        return topicExchange
    }

    @Bean("nuotraxTopicDlq")
    fun nuotraxTopicDlq(rabbitProperties: RabbitProperties): Queue {
        val queue = QueueBuilder.durable("otau-dlq").build()
        queue.setAdminsThatShouldDeclare(nuotraxTopicAmqpAdmin(rabbitProperties))
        return queue
    }

    @Bean("nuotraxTopicConnectionFactory")
    fun nuotraxTopicConnectionFactory(rabbitProperties: RabbitProperties): CachingConnectionFactory {
        val connectionFactory = CachingConnectionFactory(rabbitProperties.host, rabbitProperties.port)
        connectionFactory.virtualHost = "n"
        connectionFactory.username = rabbitProperties.username
        connectionFactory.setPassword(rabbitProperties.password)
        return connectionFactory
    }

    @Bean("nuotraxTopicDlqBinding")
    fun nuotraxTopicDlqBinding(rabbitProperties: RabbitProperties): Binding {
        val binding = BindingBuilder.bind(nuotraxTopicDlq(rabbitProperties)).to(nuotraxTopicExchange(rabbitProperties)).with(nuotraxTopicDlq(rabbitProperties).name)
        binding.setAdminsThatShouldDeclare(nuotraxTopicAmqpAdmin(rabbitProperties))
        return binding
    }

    @Bean("nuotraxTopicListenerConnectionFactory")
    fun nuotraxTopicListenerConnectionFactory(rabbitProperties: RabbitProperties, retryTemplate: RetryTemplate): SimpleRabbitListenerContainerFactory {
        val configurer = SimpleRabbitListenerContainerFactoryConfigurer(rabbitProperties)

        val factory = SimpleRabbitListenerContainerFactory() // No message converter
        configurer.configure(factory, nuotraxTopicConnectionFactory(rabbitProperties))
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

    @Bean("nuotraxTopicAmqpAdmin")
    fun nuotraxTopicAmqpAdmin(rabbitProperties: RabbitProperties): AmqpAdmin {
        val amqpAdmin = RabbitAdmin(nuotraxTopicConnectionFactory(rabbitProperties))
        amqpAdmin.afterPropertiesSet()
        return amqpAdmin
    }
}
