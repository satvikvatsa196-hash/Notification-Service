package com.notificationservice.provider

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.model.Notification

interface NotificationSender {
    /**
     * Determines if this sender supports the given channel type.
     */
    fun supports(channelType: ChannelType): Boolean

    /**
     * Sends the notification. Throws an exception if sending fails.
     */
    fun send(notification: Notification)
}
