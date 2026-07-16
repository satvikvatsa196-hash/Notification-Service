package com.notificationservice.dto.response

import com.notificationservice.domain.enums.Role
import java.util.UUID

/**
 * Returned on successful registration or login.
 *
 * @property token  Signed JWT access token (Bearer).
 * @property userId Stable user identifier for the client to store.
 * @property email  Echoed back for convenience.
 * @property role   Role granted to the authenticated principal.
 */
data class AuthResponse(
    val token: String,
    val userId: UUID,
    val email: String,
    val role: Role
)
