package com.notificationservice.messaging.consumer

import com.notificationservice.config.RabbitMQConfig
import com.notificationservice.dto.event.NotificationEvent
import com.notificationservice.exception.PermanentDeliveryException
import com.notificationservice.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
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
        var attempt = 0
        val maxAttempts = 3
        var backoffMs = 1000L
        
        while (attempt < maxAttempts) {
            try {
                notificationService.processNotification(event.notificationId)
                return // Success
            } catch (e: PermanentDeliveryException) {
                log.error("Permanent failure processing event: {}. Moving to DLQ.", event, e)
                notificationService.markAsFailed(event.notificationId, e.message ?: "Permanent failure")
                throw AmqpRejectAndDontRequeueException("Permanent failure", e)
            } catch (e: Exception) {
                attempt++
                val newRetryCount = notificationService.incrementRetryCount(event.notificationId)
                if (newRetryCount >= maxAttempts) {
                    log.error("Max retries exceeded for event: {}. Moving to DLQ.", event, e)
                    notificationService.markAsFailed(event.notificationId, "Max retries exceeded: ${e.message}")
                    throw AmqpRejectAndDontRequeueException("Max retries exceeded", e)
                }
                
                log.warn("Transient failure processing event: {}. Attempt {} of {}. Retrying in {}ms...", event, attempt, maxAttempts, backoffMs)
                try {
                    Thread.sleep(backoffMs)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw AmqpRejectAndDontRequeueException("Consumer interrupted", ie)
                }
                backoffMs *= 2 // Exponential backoff
            }
        }
    }
}
