package com.notificationservice.dto.event

import java.util.UUID

data class NotificationEvent(
    val notificationId: UUID,
    val tenantId: UUID
)
