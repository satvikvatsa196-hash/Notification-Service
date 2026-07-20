package com.notificationservice.provider

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.model.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SmsSender : NotificationSender {
    private val logger = LoggerFactory.getLogger(SmsSender::class.java)

    override fun supports(channelType: ChannelType): Boolean {
        return channelType == ChannelType.SMS
    }

    override fun send(notification: Notification) {
        logger.info("Simulating sending SMS to ${notification.recipient}. Content length: ${notification.content.length}")
        // Simulating actual network call
        Thread.sleep(100)
        logger.info("SMS sent successfully to ${notification.recipient}")
    }
}
