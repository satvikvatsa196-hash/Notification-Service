package com.notificationservice.security

import com.notificationservice.service.UserDetailsServiceImpl
import com.notificationservice.util.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Stateless JWT authentication filter that runs exactly once per HTTP request.
 *
 * How the security filter chain works:
 *
 * Spring Security wraps the servlet container in a chain of Filters managed by SecurityFilterChain.
 * Each filter in the chain can:
 *  - Inspect the request (e.g. read headers)
 *  - Short-circuit the chain (e.g. reject unauthenticated requests)
 *  - Populate the SecurityContext
 *  - Pass control to the next filter via FilterChain.doFilter
 *
 * This filter sits before UsernamePasswordAuthenticationFilter in the chain.
 *
 * Per-request authentication flow:
 *  1. Extract Bearer token from the Authorization header.
 *  2. Validate token structure and signature via JwtUtil.isTokenValid.
 *  3. Extract email (subject) and load the User from DB.
 *  4. Validate token against the loaded user (subject match + expiry check).
 *  5. Build a UsernamePasswordAuthenticationToken with granted authorities.
 *  6. Store it in SecurityContextHolder - downstream filters and controllers
 *     can retrieve it via SecurityContextHolder.getContext().authentication.
 *
 * If any step fails the request proceeds unauthenticated - the authorization
 * layer (in SecurityConfig) will then reject it with 401/403.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: UserDetailsServiceImpl
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractBearerToken(request)

        if (token != null && jwtUtil.isTokenValid(token)) {
            tryAuthenticate(token, request)
        }

        filterChain.doFilter(request, response)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts the raw JWT from the "Authorization: Bearer <token>" header.
     * Returns null if the header is absent or has an unexpected format.
     */
    private fun extractBearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ").trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Attempts to populate the SecurityContextHolder from the validated token.
     * Silently no-ops if loading the user fails, leaving the context unauthenticated.
     */
    private fun tryAuthenticate(token: String, request: HttpServletRequest) {
        // Skip if already authenticated (defensive guard)
        if (SecurityContextHolder.getContext().authentication != null) return

        try {
            val email = jwtUtil.extractEmail(token)
            val userDetails = userDetailsService.loadUserByUsername(email)

            if (jwtUtil.validateToken(token, userDetails)) {
                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,                        // credentials cleared post-auth
                    userDetails.authorities
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
                log.debug("Authenticated user '{}' via JWT", email)
            }
        } catch (ex: Exception) {
            log.debug("JWT authentication failed: {}", ex.message)
            // Do not re-throw - the request proceeds unauthenticated
        }
    }
}
