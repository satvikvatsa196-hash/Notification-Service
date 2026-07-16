package com.notificationservice.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Stateless JWT utility for token generation and validation.
 *
 * Uses HMAC-SHA256 (HS256) with a secret configured via app.jwt.secret.
 * The secret must be at least 32 characters (256 bits) for HS256.
 *
 * Token structure (standard + custom claims):
 *  - sub   -> user email (principal identifier)
 *  - role  -> e.g. "USER" or "ADMIN"
 *  - iat   -> issued-at (epoch seconds)
 *  - exp   -> expiry (epoch seconds)
 *
 * JWT Authentication flow:
 *  1. Client sends POST /auth/login -> receives signed JWT.
 *  2. Client attaches it as "Authorization: Bearer <token>" on every request.
 *  3. JwtAuthenticationFilter intercepts, calls validateToken, extracts principal.
 *  4. Spring Security populates the SecurityContext with the authenticated user.
 *  5. Authorization decisions are made by the security filter chain based on roles.
 */
@Component
class JwtUtil(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long
) {

    private val log = LoggerFactory.getLogger(JwtUtil::class.java)

    /**
     * Derives a spec-compliant HMAC-SHA key from the configured secret.
     * Evaluated lazily and reused for all sign/verify operations.
     */
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    // ── Token Generation ─────────────────────────────────────────────────────

    /**
     * Generates a signed JWT for the given userDetails.
     *
     * @param userDetails the authenticated principal - email used as subject.
     * @param role        string role name (e.g. "ADMIN") embedded as a custom claim.
     * @return signed compact JWT string.
     */
    fun generateToken(userDetails: UserDetails, role: String): String {
        val now = Instant.now()
        val expiry = now.plus(expirationMs, ChronoUnit.MILLIS)

        return Jwts.builder()
            .subject(userDetails.username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact()
    }

    // ── Token Validation & Extraction ────────────────────────────────────────

    /**
     * Returns true if the token's signature is valid, it is not expired,
     * and the subject matches userDetails.
     */
    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        return try {
            val email = extractEmail(token)
            email == userDetails.username && !isTokenExpired(token)
        } catch (ex: JwtException) {
            log.debug("JWT validation failed: {}", ex.message)
            false
        } catch (ex: IllegalArgumentException) {
            log.debug("JWT validation failed (illegal argument): {}", ex.message)
            false
        }
    }

    /**
     * Validates token signature and expiry WITHOUT tying to a specific user.
     * Used during filter chain processing before loading UserDetails.
     */
    fun isTokenValid(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (ex: JwtException) {
            log.debug("Token is invalid or expired: {}", ex.message)
            false
        } catch (ex: IllegalArgumentException) {
            log.debug("Token is blank or malformed (illegal argument): {}", ex.message)
            false
        }
    }

    /** Extracts the sub claim (email) from the token. */
    fun extractEmail(token: String): String =
        parseClaims(token).subject

    /** Extracts the custom role claim from the token. */
    fun extractRole(token: String): String =
        parseClaims(token)["role"] as String

    // ── Private Helpers ──────────────────────────────────────────────────────

    private fun isTokenExpired(token: String): Boolean =
        parseClaims(token).expiration.before(Date())

    /**
     * Parses and verifies the JWT signature.
     * Throws JwtException (or subtype) on any failure.
     */
    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
