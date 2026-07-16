package com.notificationservice.unit

import com.notificationservice.domain.enums.Role
import com.notificationservice.domain.model.User
import com.notificationservice.util.JwtUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [JwtUtil].
 *
 * Uses a fixed 64-character secret (>= 32 chars required by HS256).
 */
class JwtUtilTest {

    companion object {
        private const val SECRET = "test-jwt-secret-that-is-long-enough-for-hs256-algorithm-here!"
        private const val EXPIRATION_MS = 3_600_000L  // 1 hour
    }

    private lateinit var jwtUtil: JwtUtil
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        jwtUtil = JwtUtil(SECRET, EXPIRATION_MS)

        testUser = User(
            email = "test@example.com",
            passwordHash = "\$2a\$10\$dummyhash",
            role = Role.USER
        )
    }

    // ── Token Generation ─────────────────────────────────────────────────────

    @Test
    fun `generateToken should produce a non-blank token`() {
        val token = jwtUtil.generateToken(testUser, testUser.role.name)
        assertThat(token).isNotBlank()
    }

    @Test
    fun `generateToken should embed the email as subject`() {
        val token = jwtUtil.generateToken(testUser, Role.USER.name)
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@example.com")
    }

    @Test
    fun `generateToken should embed the role claim`() {
        val token = jwtUtil.generateToken(testUser, Role.ADMIN.name)
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN")
    }

    @Test
    fun `generateToken for USER role should embed USER in role claim`() {
        val token = jwtUtil.generateToken(testUser, Role.USER.name)
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER")
    }

    // ── Token Validation ─────────────────────────────────────────────────────

    @Test
    fun `validateToken should return true for a fresh token matching the user`() {
        val token = jwtUtil.generateToken(testUser, Role.USER.name)
        assertThat(jwtUtil.validateToken(token, testUser)).isTrue()
    }

    @Test
    fun `validateToken should return false when user email does not match token subject`() {
        val token = jwtUtil.generateToken(testUser, Role.USER.name)

        val anotherUser = User(
            email = "other@example.com",
            passwordHash = "\$2a\$10\$dummyhash"
        )

        assertThat(jwtUtil.validateToken(token, anotherUser)).isFalse()
    }

    @Test
    fun `isTokenValid should return true for a fresh token`() {
        val token = jwtUtil.generateToken(testUser, Role.USER.name)
        assertThat(jwtUtil.isTokenValid(token)).isTrue()
    }

    @Test
    fun `isTokenValid should return false for a tampered token`() {
        val token = jwtUtil.generateToken(testUser, Role.USER.name)
        val tampered = token.dropLast(5) + "XXXXX"
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse()
    }

    @Test
    fun `isTokenValid should return false for an expired token`() {
        // Create a JwtUtil with 0ms expiry — token is immediately expired
        val expiredJwtUtil = JwtUtil(SECRET, -1L)
        val token = expiredJwtUtil.generateToken(testUser, Role.USER.name)

        // Wait a moment for the expiry to kick in
        Thread.sleep(10)

        assertThat(jwtUtil.isTokenValid(token)).isFalse()
    }

    @Test
    fun `isTokenValid should return false for a completely garbage string`() {
        assertThat(jwtUtil.isTokenValid("not.a.jwt")).isFalse()
    }

    @Test
    fun `isTokenValid should return false for an empty string`() {
        assertThat(jwtUtil.isTokenValid("")).isFalse()
    }

    // ── Claim Extraction ─────────────────────────────────────────────────────

    @Test
    fun `extractEmail should throw on invalid token`() {
        assertThatThrownBy { jwtUtil.extractEmail("invalid.token.here") }
    }
}
