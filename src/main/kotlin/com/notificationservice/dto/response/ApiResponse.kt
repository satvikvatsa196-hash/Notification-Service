package com.notificationservice.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * Generic API response envelope for all successful responses.
 *
 * @param T The type of the response data payload
 * @param data The actual response payload (null for void responses)
 * @param message Optional human-readable status message
 * @param meta Optional pagination or additional metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean = true,
    val timestamp: Instant = Instant.now(),
    val message: String? = null,
    val data: T? = null,
    val meta: PageMeta? = null
) {
    companion object {
        fun <T> ok(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(data = data, message = message)

        fun <T> ok(data: T, meta: PageMeta, message: String? = null): ApiResponse<T> =
            ApiResponse(data = data, meta = meta, message = message)

        fun <T> created(data: T, message: String? = "Resource created successfully"): ApiResponse<T> =
            ApiResponse(data = data, message = message)

        fun noContent(message: String = "Operation completed successfully"): ApiResponse<Nothing> =
            ApiResponse(message = message)
    }
}

/**
 * Pagination metadata returned with list responses.
 */
data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean
)
