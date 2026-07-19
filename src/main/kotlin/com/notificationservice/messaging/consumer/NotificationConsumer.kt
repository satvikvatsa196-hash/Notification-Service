package com.notificationservice.messaging.consumer

import com.notificationservice.config.RabbitMQConfig
import com.notificationservice.dto.event.NotificationEvent
import com.notificationservice.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class NotificationConsumer(
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(NotificationConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_NAME])
    fun consumeNotificationEvent(event: NotificationEvent) {
        log.info("Received notification event: {}", event)
        try {
            notificationService.processNotification(event.notificationId)
        } catch (e: Exception) {
            log.error("Failed to process notification event: {}", event, e)
            // Depending on requirements, we could rethrow for DLQ or handle it here
            // Throwing an exception here will cause the message to be requeued or sent to a DLQ (if configured)
            throw e
        }
    }
}
