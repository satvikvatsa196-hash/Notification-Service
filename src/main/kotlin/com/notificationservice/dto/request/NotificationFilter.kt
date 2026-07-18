package com.notificationservice.dto.request

import com.notificationservice.domain.enums.NotificationStatus
import java.time.Instant
import java.util.UUID

data class NotificationFilter(
    val status: NotificationStatus? = null,
    val channelId: UUID? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null
)
