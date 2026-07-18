package com.notificationservice.dto.response

import com.notificationservice.domain.enums.NotificationStatus
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val tenantId: UUID,
    val channelId: UUID,
    val recipient: String,
    val subject: String?,
    val content: String,
    val status: NotificationStatus,
    val scheduledAt: Instant?,
    val sentAt: Instant?,
    val errorDetails: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)
