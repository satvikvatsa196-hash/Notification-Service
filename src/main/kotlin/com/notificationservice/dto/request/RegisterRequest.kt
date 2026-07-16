package com.notificationservice.dto.request

import com.notificationservice.domain.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for POST /api/v1/auth/register.
 */
data class RegisterRequest(

    @field:Email(message = "Must be a valid email address")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val password: String,

    /**
     * Optional role — defaults to [Role.USER] when omitted.
     * Only ADMIN users may create other ADMINs (enforced in [AuthService]).
     */
    val role: Role = Role.USER
)
