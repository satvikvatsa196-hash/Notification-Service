package com.notificationservice.controller

import com.notificationservice.dto.request.LoginRequest
import com.notificationservice.dto.request.RegisterRequest
import com.notificationservice.dto.response.ApiResponse
import com.notificationservice.dto.response.AuthResponse
import com.notificationservice.exception.ErrorResponse
import com.notificationservice.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Handles user registration and login.
 *
 * All endpoints under /api/v1/auth/ are public (no JWT required).
 * See SecurityConfig for the permit rule.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and login - returns JWT tokens")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a user account and returns a signed JWT. Default role is USER."
    )
    @ApiResponses(
        SwaggerResponse(responseCode = "201", description = "User registered successfully"),
        SwaggerResponse(
            responseCode = "409",
            description = "Email already in use",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "422",
            description = "Validation error",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.register(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(authResponse, "User registered successfully"))
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login with email and password",
        description = "Validates credentials and returns a signed JWT for subsequent requests."
    )
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "Login successful"),
        SwaggerResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "422",
            description = "Validation error",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login successful"))
    }
}
