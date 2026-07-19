package com.notificationservice.messaging.producer

import com.notificationservice.config.RabbitMQConfig
import com.notificationservice.dto.event.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class NotificationProducer(
    private val rabbitTemplate: RabbitTemplate
) {
    private val log = LoggerFactory.getLogger(NotificationProducer::class.java)

    fun sendNotificationEvent(event: NotificationEvent) {
        log.info("Sending notification event to RabbitMQ: {}", event)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY,
            event
        )
    }
}
