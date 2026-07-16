package com.notificationservice.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Request body for POST /api/v1/auth/login.
 */
data class LoginRequest(

    @field:Email(message = "Must be a valid email address")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)
