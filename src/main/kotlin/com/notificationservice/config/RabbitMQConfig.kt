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
        
        const val DLX_NAME = "notification.dlx"
        const val DLQ_NAME = "notification.dlq"
        const val DLQ_ROUTING_KEY = "notification.dlq.routing.key"
    }

    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    @Bean
    fun queue(): Queue {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build()
    }

    @Bean
    fun binding(queue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY)
    }

    @Bean
    fun dlxExchange(): DirectExchange {
        return DirectExchange(DLX_NAME)
    }

    @Bean
    fun dlqQueue(): Queue {
        return org.springframework.amqp.core.QueueBuilder.durable(DLQ_NAME).build()
    }

    @Bean
    fun dlqBinding(dlqQueue: Queue, dlxExchange: DirectExchange): Binding {
        return BindingBuilder.bind(dlqQueue).to(dlxExchange).with(DLQ_ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
