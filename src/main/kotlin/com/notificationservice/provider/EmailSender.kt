package com.notificationservice.provider

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.model.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmailSender : NotificationSender {
    private val logger = LoggerFactory.getLogger(EmailSender::class.java)

    override fun supports(channelType: ChannelType): Boolean {
        return channelType == ChannelType.EMAIL
    }

    private val attemptMap = mutableMapOf<java.util.UUID, Int>()

    override fun send(notification: Notification) {
        val attempts = attemptMap.getOrDefault(notification.id!!, 0)
        attemptMap[notification.id!!] = attempts + 1

        if (notification.recipient == "permanent@example.com") {
            throw com.notificationservice.exception.PermanentDeliveryException("Simulated permanent failure")
        }

        if (notification.recipient == "exhaustion@example.com") {
            throw com.notificationservice.exception.TransientDeliveryException("Simulated transient failure")
        }

        if (notification.recipient == "retry-success@example.com" && attempts < 1) {
            throw com.notificationservice.exception.TransientDeliveryException("Simulated transient failure on first attempt")
        }

        logger.info("Simulating sending EMAIL to ${notification.recipient}. Subject: ${notification.subject}")
        Thread.sleep(100)
        logger.info("EMAIL sent successfully to ${notification.recipient}")
    }
}
