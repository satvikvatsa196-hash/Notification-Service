package com.notificationservice.dto.response

import com.notificationservice.domain.enums.ChannelType
import java.time.Instant
import java.util.UUID

/**
 * Response DTO for a Channel resource.
 * The config map is included but sensitive fields should be masked
 * at the service layer before populating this DTO.
 */
data class ChannelResponse(
    val id: UUID,
    val tenantId: UUID,
    val channelType: ChannelType,
    val name: String,
    val config: Map<String, Any>,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)
