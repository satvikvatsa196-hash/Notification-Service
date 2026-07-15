package com.notificationservice.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request body for creating a new Tenant.
 */
data class CreateTenantRequest(

    @field:NotBlank(message = "Name must not be blank")
    @field:Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    val name: String,

    @field:NotBlank(message = "Slug must not be blank")
    @field:Size(min = 2, max = 100, message = "Slug must be between 2 and 100 characters")
    @field:Pattern(
        regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
        message = "Slug must contain only lowercase letters, digits, and hyphens, and cannot start or end with a hyphen"
    )
    val slug: String,

    @field:NotBlank(message = "Contact email must not be blank")
    @field:Email(message = "Contact email must be a valid email address")
    @field:Size(max = 320, message = "Contact email must not exceed 320 characters")
    val contactEmail: String
)
