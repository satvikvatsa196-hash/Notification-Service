package com.notificationservice.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateNotificationRequest(
    @field:NotNull(message = "Channel ID is required")
    val channelId: UUID,

    @field:NotBlank(message = "Recipient is required")
    val recipient: String,

    val subject: String? = null,

    @field:NotBlank(message = "Content is required")
    val content: String,

    val scheduledAt: Instant? = null
)
