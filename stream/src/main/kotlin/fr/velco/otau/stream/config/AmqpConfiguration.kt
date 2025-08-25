package fr.velco.otau.stream.config

import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableRabbit //Mandatory to create queues on start-up
@EnableAutoConfiguration(exclude = [RabbitAutoConfiguration::class]) //Exclude org.springframework.boot.autoconfigure.amqp.RabbitAnnotationDrivenConfiguration because use specific config for each exchange
class AmqpConfiguration {
    @Bean
    fun retryTemplate(): RetryTemplate { // Common retryTemplate which manages specific retries for some exceptions
        val retryTemplate = RetryTemplate()

        val exceptionClassifierRetryPolicy = ExceptionClassifierRetryPolicy()
        retryTemplate.setRetryPolicy(exceptionClassifierRetryPolicy)

        val backOffPolicy = ExponentialBackOffPolicy().apply {
            initialInterval = 5 //In seconds
            maxInterval = 300 //In seconds
            multiplier = 2.0
        }
        retryTemplate.setBackOffPolicy(backOffPolicy)

        return retryTemplate
    }
}
