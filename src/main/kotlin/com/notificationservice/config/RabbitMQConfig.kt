package com.notificationservice.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME = "notification.exchange"
        const val QUEUE_NAME = "notification.queue"
        const val ROUTING_KEY = "notification.routing.key"
    }

    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    @Bean
    fun queue(): Queue {
        return Queue(QUEUE_NAME, true) // Durable queue
    }

    @Bean
    fun binding(queue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
