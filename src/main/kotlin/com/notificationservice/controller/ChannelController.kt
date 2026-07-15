package com.notificationservice.controller

import com.notificationservice.dto.request.CreateChannelRequest
import com.notificationservice.dto.response.ApiResponse
import com.notificationservice.dto.response.ChannelResponse
import com.notificationservice.service.ChannelService
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
@RequestMapping("/api/v1/tenants/{tenantId}/channels")
@Tag(name = "Channels", description = "Manage delivery channels for a tenant")
class ChannelController(
    private val channelService: ChannelService
) {

    @GetMapping
    @Operation(summary = "List channels for a tenant")
    fun listChannels(
        @Parameter(description = "Tenant UUID", required = true) @PathVariable tenantId: UUID,
        @ParameterObject @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<List<ChannelResponse>>> {
        val (channels, meta) = channelService.listChannelsForTenant(tenantId, pageable)
        return ResponseEntity.ok(ApiResponse.ok(channels, meta))
    }

    @GetMapping("/{channelId}")
    @Operation(summary = "Get a channel by ID")
    fun getChannel(
        @Parameter(description = "Tenant UUID", required = true) @PathVariable tenantId: UUID,
        @Parameter(description = "Channel UUID", required = true) @PathVariable channelId: UUID
    ): ResponseEntity<ApiResponse<ChannelResponse>> {
        val channel = channelService.getChannelById(tenantId, channelId)
        return ResponseEntity.ok(ApiResponse.ok(channel))
    }

    @PostMapping
    @Operation(
        summary = "Create a channel for a tenant",
        description = "Creates a new delivery channel. Each (name, channelType) combination must be unique per tenant."
    )
    fun createChannel(
        @Parameter(description = "Tenant UUID", required = true) @PathVariable tenantId: UUID,
        @Valid @RequestBody request: CreateChannelRequest
    ): ResponseEntity<ApiResponse<ChannelResponse>> {
        val channel = channelService.createChannel(tenantId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(channel))
    }
}
