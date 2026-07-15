package com.notificationservice.dto.response

import java.time.Instant
import java.util.UUID

/**
 * Response DTO for a Tenant resource.
 * Only exposes safe, non-sensitive fields to API consumers.
 */
data class TenantResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val contactEmail: String,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)
