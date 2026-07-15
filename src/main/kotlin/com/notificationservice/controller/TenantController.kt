package com.notificationservice.controller

import com.notificationservice.dto.request.CreateTenantRequest
import com.notificationservice.dto.response.ApiResponse
import com.notificationservice.dto.response.TenantResponse
import com.notificationservice.service.TenantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Manage tenants that use the notification service")
class TenantController(
    private val tenantService: TenantService
) {

    @GetMapping
    @Operation(
        summary = "List all active tenants",
        description = "Returns a paginated list of all active tenants ordered by creation date (newest first)."
    )
    fun listTenants(
        @ParameterObject @PageableDefault(size = 20, sort = ["createdAt"]) pageable: Pageable
    ): ResponseEntity<ApiResponse<List<TenantResponse>>> {
        val (tenants, meta) = tenantService.listTenants(pageable)
        return ResponseEntity.ok(ApiResponse.ok(tenants, meta))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tenant by ID")
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant found"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Tenant not found",
            content = [Content(schema = Schema(implementation = com.notificationservice.exception.ErrorResponse::class))]
        )
    )
    fun getTenantById(
        @Parameter(description = "Tenant UUID", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<TenantResponse>> {
        val tenant = tenantService.getTenantById(id)
        return ResponseEntity.ok(ApiResponse.ok(tenant))
    }

    @PostMapping
    @Operation(
        summary = "Create a new tenant",
        description = "Creates a new tenant. Name and slug must be globally unique."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant created"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Tenant with the same name or slug already exists",
            content = [Content(schema = Schema(implementation = com.notificationservice.exception.ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = com.notificationservice.exception.ErrorResponse::class))]
        )
    )
    fun createTenant(
        @Valid @RequestBody request: CreateTenantRequest
    ): ResponseEntity<ApiResponse<TenantResponse>> {
        val tenant = tenantService.createTenant(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(tenant))
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deactivate a tenant",
        description = "Soft-deletes a tenant by setting its active flag to false. No data is physically deleted."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant deactivated"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    )
    fun deactivateTenant(
        @Parameter(description = "Tenant UUID", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        tenantService.deactivateTenant(id)
        return ResponseEntity.ok(ApiResponse.noContent("Tenant deactivated successfully"))
    }
}
