package com.notificationservice.service

import com.notificationservice.domain.enums.Role
import com.notificationservice.domain.model.User
import com.notificationservice.dto.request.LoginRequest
import com.notificationservice.dto.request.RegisterRequest
import com.notificationservice.dto.response.AuthResponse
import com.notificationservice.exception.EmailAlreadyExistsException
import com.notificationservice.repository.UserRepository
import com.notificationservice.util.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Core authentication service handling registration and login.
 *
 * ## Why BCrypt?
 * BCrypt is an adaptive password hashing algorithm designed to be intentionally slow.
 * It incorporates a salt automatically (preventing rainbow-table attacks) and has a
 * configurable work factor that can be increased as hardware gets faster.
 * Spring Security's [PasswordEncoder] abstraction means we can swap algorithms
 * without changing service code.
 *
 * ## Registration flow
 *  1. Validate email uniqueness.
 *  2. Hash the password with BCrypt.
 *  3. Persist the [User] entity.
 *  4. Generate and return a signed JWT.
 *
 * ## Login flow
 *  1. Delegate credential verification to [AuthenticationManager] (Spring Security).
 *  2. On success, load the [User] entity from the database.
 *  3. Generate and return a signed JWT.
 */
@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val authenticationManager: AuthenticationManager
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Registers a new user.
     *
     * @throws EmailAlreadyExistsException if the email is already taken.
     */
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            role = Role.USER
        )

        val saved = userRepository.save(user)
        log.info("Registered new user [id={}, role={}]", saved.id, saved.role)

        val token = jwtUtil.generateToken(saved, saved.role.name)
        return saved.toAuthResponse(token)
    }

    /**
     * Authenticates a user and issues a JWT.
     *
     * @throws org.springframework.security.core.AuthenticationException
     *   (specifically [org.springframework.security.authentication.BadCredentialsException])
     *   if credentials are invalid — Spring Security throws this automatically.
     */
    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        // Throws BadCredentialsException if email/password don't match
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalStateException("User vanished after authentication — this is a bug") }

        log.info("User logged in [id={}, role={}]", user.id, user.role)

        val token = jwtUtil.generateToken(user, user.role.name)
        return user.toAuthResponse(token)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun User.toAuthResponse(token: String) = AuthResponse(
        token = token,
        userId = id!!,
        email = email,
        role = role
    )
}
