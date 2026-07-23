package com.notificationservice.controller

import com.notificationservice.dto.request.CreateNotificationRequest
import com.notificationservice.dto.request.NotificationFilter
import com.notificationservice.dto.response.ApiResponse
import com.notificationservice.dto.response.NotificationResponse
import com.notificationservice.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/notifications")
@Tag(name = "Notifications", description = "Manage and query notifications for a tenant")
class NotificationController(
    private val notificationService: NotificationService
) {

    @PostMapping
    @Operation(summary = "Create a notification")
    fun createNotification(
        @Parameter(description = "Tenant UUID", required = true) @PathVariable tenantId: UUID,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody request: CreateNotificationRequest
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val notification = notificationService.createNotification(tenantId, request, idempotencyKey)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(notification))
    }

    @GetMapping
    @Operation(summary = "Get notification history with filters and pagination")
    fun getNotificationHistory(
        @Parameter(description = "Tenant UUID", required = true) @PathVariable tenantId: UUID,
        @ParameterObject filter: NotificationFilter,
        @ParameterObject @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<List<NotificationResponse>>> {
        val (notifications, meta) = notificationService.listNotifications(tenantId, filter, pageable)
        return ResponseEntity.ok(ApiResponse.ok(notifications, meta))
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID")
    fun getNotificationById(
        @Parameter(description = "Tenant UUID", required = true) @PathVariable tenantId: UUID,
        @Parameter(description = "Notification UUID", required = true) @PathVariable notificationId: UUID
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val notification = notificationService.getNotificationById(tenantId, notificationId)
        return ResponseEntity.ok(ApiResponse.ok(notification))
    }
}
