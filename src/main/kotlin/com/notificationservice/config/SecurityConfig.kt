package com.notificationservice.config

import com.notificationservice.security.JwtAuthenticationFilter
import com.notificationservice.service.UserDetailsServiceImpl
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security configuration for the Notification Service.
 *
 * How authorization decisions are made:
 * Every incoming request passes through the SecurityFilterChain:
 *  1. JwtAuthenticationFilter runs first - populates SecurityContextHolder
 *     with the authenticated principal (or leaves it empty for anonymous requests).
 *  2. AuthorizationFilter runs next and evaluates the rules defined in securityFilterChain:
 *      - PUBLIC paths -> permitAll() - no token required.
 *      - ADMIN-only paths -> hasRole("ADMIN") - requires "ROLE_ADMIN" authority.
 *      - Any other path -> authenticated() - any valid JWT is sufficient.
 *  3. If the principal lacks the required authority -> HTTP 403 Forbidden.
 *  4. If there is no principal at all -> HTTP 401 Unauthorized.
 *
 * @EnableMethodSecurity additionally enables @PreAuthorize / @PostAuthorize
 * annotations on individual service or controller methods for fine-grained control.
 *
 * Session strategy:
 * The service is fully stateless (STATELESS session policy). No HttpSession is
 * created or used. Every request must carry a valid JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    private val userDetailsService: UserDetailsServiceImpl,
    private val appProperties: AppProperties
) {

    // ── Password Encoder ─────────────────────────────────────────────────────

    /**
     * BCrypt with the default strength factor (10 rounds).
     *
     * Why BCrypt?
     *  - Adaptive: the work factor can be increased as hardware improves.
     *  - Salt: automatically generates and embeds a random 128-bit salt per hash.
     *  - Slow by design: makes brute-force attacks computationally expensive.
     *  - Widely audited: the de-facto standard in the industry.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // ── Authentication Provider ───────────────────────────────────────────────

    /**
     * Wires UserDetailsServiceImpl and BCryptPasswordEncoder together.
     * Used internally by AuthenticationManager when verifying credentials.
     */
    @Bean
    fun authenticationProvider(): AuthenticationProvider =
        DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder())
        }

    /**
     * Exposes the AuthenticationManager as a Spring Bean so it can be
     * injected into AuthService.
     */
    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    // ── Security Filter Chain ────────────────────────────────────────────────

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable CSRF - not needed for stateless JWT APIs
            .csrf { it.disable() }

            // Configure CORS using the bean defined below
            .cors { it.configurationSource(corsConfigurationSource()) }

            // Stateless session - no HttpSession, no cookies
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // Register our JWT filter before Spring's username/password filter
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

            // Custom error handlers that return JSON instead of HTML redirects
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
            }

            // Authorization rules (order matters - most specific first)
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints - no authentication required
                    .requestMatchers(
                        "/api/v1/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/actuator/health",
                        "/actuator/info"
                    ).permitAll()

                    // Admin-only endpoints
                    .requestMatchers(
                        HttpMethod.DELETE, "/api/v1/tenants/**"
                    ).hasRole("ADMIN")

                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/tenants"
                    ).hasRole("ADMIN")

                    // All other requests require a valid JWT (any role)
                    .anyRequest().authenticated()
            }

        return http.build()
    }

    // ── CORS ─────────────────────────────────────────────────────────────────

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = appProperties.cors.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    // ── Custom error handlers ─────────────────────────────────────────────────

    /**
     * Returns HTTP 401 with a JSON body when a request is unauthenticated.
     * Prevents Spring Security from redirecting to a login page (which does not exist in an API).
     */
    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            response.contentType = "application/json"
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write(
                """{"status":401,"error":"Unauthorized","message":"Authentication required. Provide a valid Bearer token."}"""
            )
        }

    /**
     * Returns HTTP 403 with a JSON body when an authenticated user lacks the required role.
     */
    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _, response, _ ->
            response.contentType = "application/json"
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.writer.write(
                """{"status":403,"error":"Forbidden","message":"You do not have permission to access this resource."}"""
            )
        }
}
