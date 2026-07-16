package com.notificationservice.unit

import com.notificationservice.domain.enums.Role
import com.notificationservice.domain.model.User
import com.notificationservice.dto.request.LoginRequest
import com.notificationservice.dto.request.RegisterRequest
import com.notificationservice.exception.EmailAlreadyExistsException
import com.notificationservice.repository.UserRepository
import com.notificationservice.service.AuthService
import com.notificationservice.util.JwtUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val jwtUtil: JwtUtil = mockk()
    private val authenticationManager: AuthenticationManager = mockk()

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(userRepository, passwordEncoder, jwtUtil, authenticationManager)
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    fun `register should save user and return auth response with token`() {
        val request = RegisterRequest(
            email = "newuser@example.com",
            password = "securepass123"
        )
        val hashedPassword = "\$2a\$10\$hashed"
        val fakeToken = "fake.jwt.token"
        val savedUser = User(
            email = request.email,
            passwordHash = hashedPassword,
            role = Role.USER
        ).apply {
            // Simulate JPA-assigned ID
            val idField = User::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, UUID.randomUUID())
        }

        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns hashedPassword
        every { userRepository.save(any()) } returns savedUser
        every { jwtUtil.generateToken(savedUser, Role.USER.name) } returns fakeToken

        val result = authService.register(request)

        assertThat(result.token).isEqualTo(fakeToken)
        assertThat(result.email).isEqualTo(request.email)
        assertThat(result.role).isEqualTo(Role.USER)
        verify(exactly = 1) { userRepository.save(any()) }
        verify(exactly = 1) { passwordEncoder.encode(request.password) }
    }

    @Test
    fun `register should throw EmailAlreadyExistsException when email is taken`() {
        val request = RegisterRequest(
            email = "existing@example.com",
            password = "securepass123"
        )

        every { userRepository.existsByEmail(request.email) } returns true

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(EmailAlreadyExistsException::class.java)
            .hasMessageContaining("existing@example.com")

        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
    }

    @Test
    fun `register with ADMIN role should create ADMIN user`() {
        val request = RegisterRequest(
            email = "admin@example.com",
            password = "adminpass123",
            role = Role.ADMIN
        )
        val hashedPassword = "\$2a\$10\$hashed"
        val fakeToken = "admin.jwt.token"
        val savedAdmin = User(
            email = request.email,
            passwordHash = hashedPassword,
            role = Role.ADMIN
        ).apply {
            val idField = User::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, UUID.randomUUID())
        }

        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns hashedPassword
        every { userRepository.save(any()) } returns savedAdmin
        every { jwtUtil.generateToken(savedAdmin, Role.ADMIN.name) } returns fakeToken

        val result = authService.register(request)

        assertThat(result.role).isEqualTo(Role.ADMIN)
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    fun `login should return auth response with token on valid credentials`() {
        val request = LoginRequest(email = "user@example.com", password = "password123")
        val fakeToken = "valid.jwt.token"
        val existingUser = User(
            email = request.email,
            passwordHash = "\$2a\$10\$hashed",
            role = Role.USER
        ).apply {
            val idField = User::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, UUID.randomUUID())
        }

        every {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )
        } returns UsernamePasswordAuthenticationToken(existingUser, null, existingUser.authorities)

        every { userRepository.findByEmail(request.email) } returns Optional.of(existingUser)
        every { jwtUtil.generateToken(existingUser, Role.USER.name) } returns fakeToken

        val result = authService.login(request)

        assertThat(result.token).isEqualTo(fakeToken)
        assertThat(result.email).isEqualTo(request.email)
        assertThat(result.role).isEqualTo(Role.USER)
    }

    @Test
    fun `login should propagate BadCredentialsException on wrong password`() {
        val request = LoginRequest(email = "user@example.com", password = "wrongpass")

        every {
            authenticationManager.authenticate(any())
        } throws BadCredentialsException("Bad credentials")

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(BadCredentialsException::class.java)

        verify(exactly = 0) { userRepository.findByEmail(any()) }
    }
}
