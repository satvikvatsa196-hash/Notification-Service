package com.notificationservice.exception

import java.time.Instant

/**
 * Standardized error response envelope returned for all API errors.
 * Follows RFC 7807 Problem Details for HTTP APIs.
 */
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: List<FieldError> = emptyList()
) {
    data class FieldError(
        val field: String,
        val message: String,
        val rejectedValue: Any? = null
    )
}
