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

    override fun send(notification: Notification) {
        logger.info("Simulating sending EMAIL to ${notification.recipient}. Subject: ${notification.subject}")
        // Simulating actual network call
        Thread.sleep(100)
        logger.info("EMAIL sent successfully to ${notification.recipient}")
    }
}
