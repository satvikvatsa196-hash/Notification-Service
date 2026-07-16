package com.notificationservice.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.notificationservice.domain.enums.Role
import com.notificationservice.dto.request.LoginRequest
import com.notificationservice.dto.request.RegisterRequest
import com.notificationservice.repository.UserRepository
import com.notificationservice.util.JwtUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * Comprehensive authentication and authorization integration test suite.
 *
 * Uses H2 in-memory database (profile: h2test) — no Docker required.
 * All tests execute through the real Spring Security filter chain.
 * No SecurityContext mocking — real JWT tokens are generated and validated.
 *
 * Coverage:
 *  - Registration: success, duplicate email, validation failures
 *  - Login: success, wrong password, non-existing user, validation failures
 *  - JWT authentication: no token, invalid token, malformed token, valid token
 *  - Authorization: USER vs ADMIN role enforcement on all protected endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthSecurityIT {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jwtUtil: JwtUtil
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private val userEmail    = "user@security-test.com"
    private val adminEmail   = "admin@security-test.com"
    private val userPassword = "userPass123"
    private val adminPassword = "adminPass456"

    /** Tokens populated in @BeforeEach after re-registering users. */
    private var userToken  = ""
    private var adminToken = ""

    @BeforeEach
    fun resetDatabaseAndSeedUsers() {
        // Wipe all users so each test starts from a clean slate
        userRepository.deleteAll()

        // Register USER
        userToken = registerAndGetToken(userEmail, userPassword, Role.USER)

        // Create ADMIN directly in the test database
        val admin = com.notificationservice.domain.model.User(
            email = adminEmail,
            passwordHash = passwordEncoder.encode(adminPassword),
            role = Role.ADMIN
        )
        val savedAdmin = userRepository.save(admin)
        adminToken = jwtUtil.generateToken(savedAdmin, savedAdmin.role.name)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun registerAndGetToken(email: String, password: String, role: Role): String {
        val body = objectMapper.writeValueAsString(
            RegisterRequest(email = email, password = password, role = role)
        )
        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andReturn()
        return objectMapper
            .readTree(result.response.contentAsString)["data"]["token"].asText()
    }

    private fun json(obj: Any) = objectMapper.writeValueAsString(obj)

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRATION
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class Registration {

        @Test
        fun `successful USER registration returns 201 with token and role`() {
            val req = RegisterRequest("new-user@test.com", "strongPass1")

            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.token") { isString() }
                jsonPath("$.data.email") { value("new-user@test.com") }
                jsonPath("$.data.role") { value("USER") }
                jsonPath("$.data.userId") { isString() }
            }
        }

        @Test
fun `registration with ADMIN role request still creates USER`() {
    val req = RegisterRequest(
        "fake-admin@test.com",
        "strongPass1",
        Role.ADMIN
    )

    mockMvc.post("/api/v1/auth/register") {
        contentType = MediaType.APPLICATION_JSON
        content = json(req)
    }.andExpect {
        status { isCreated() }
        jsonPath("$.data.role") { value("USER") }
    }
}

        @Test
        fun `duplicate email registration returns 409 Conflict`() {
            // userEmail already registered in @BeforeEach
            val req = RegisterRequest(userEmail, "anotherPass123")

            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error") { value("Conflict") }
                jsonPath("$.message") { value(org.hamcrest.Matchers.containsString(userEmail)) }
            }
        }

        @Test
        fun `blank email returns 422 Validation Failed`() {
            val payload = mapOf("email" to "", "password" to "validPass1")

            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(payload)
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.error") { value("Validation Failed") }
            }
        }

        @Test
        fun `invalid email format returns 422 Validation Failed`() {
            val payload = mapOf("email" to "not-an-email", "password" to "validPass1")

            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(payload)
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.error") { value("Validation Failed") }
                jsonPath("$.details[0].field") { value("email") }
            }
        }

        @Test
        fun `password shorter than 8 characters returns 422 Validation Failed`() {
            val payload = mapOf("email" to "short-pw@test.com", "password" to "short")

            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(payload)
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.error") { value("Validation Failed") }
                jsonPath("$.details[0].field") { value("password") }
            }
        }

        @Test
        fun `blank password returns 422 Validation Failed`() {
            val payload = mapOf("email" to "blankpw@test.com", "password" to "")

            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(payload)
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.error") { value("Validation Failed") }
            }
        }

        @Test
        fun `missing request body returns 400`() {
            mockMvc.post("/api/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = ""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class Login {

        @Test
        fun `valid credentials return 200 with JWT token`() {
            val req = LoginRequest(userEmail, userPassword)

            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.token") { isString() }
                jsonPath("$.data.email") { value(userEmail) }
                jsonPath("$.data.role") { value("USER") }
            }
        }

        @Test
        fun `login response token is a valid parseable JWT`() {
            val req = LoginRequest(userEmail, userPassword)

            val result = mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andReturn()

            val token = objectMapper
                .readTree(result.response.contentAsString)["data"]["token"].asText()

            // Use the real JwtUtil — validates signature + expiry
            assert(jwtUtil.isTokenValid(token)) { "Token returned by /login must be valid" }
            assert(jwtUtil.extractEmail(token) == userEmail)
            assert(jwtUtil.extractRole(token) == "USER")
        }

        @Test
        fun `wrong password returns 401 Unauthorized`() {
            val req = LoginRequest(userEmail, "wrongPassword!")

            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `non-existing user returns 401 Unauthorized`() {
            val req = LoginRequest("ghost@nobody.com", "anyPassword1")

            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `blank email in login request returns 422`() {
            val payload = mapOf("email" to "", "password" to "somePass123")

            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(payload)
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.error") { value("Validation Failed") }
            }
        }

        @Test
        fun `blank password in login request returns 422`() {
            val payload = mapOf("email" to userEmail, "password" to "")

            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(payload)
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.error") { value("Validation Failed") }
            }
        }

        @Test
        fun `admin credentials return role ADMIN in response`() {
            val req = LoginRequest(adminEmail, adminPassword)

            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(req)
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.role") { value("ADMIN") }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JWT AUTHENTICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class JwtAuthentication {

        @Test
        fun `no Authorization header returns 401`() {
            mockMvc.get("/api/v1/tenants")
                .andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `completely invalid token returns 401`() {
            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer not.a.real.jwt")
            }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `malformed Authorization header (no Bearer prefix) returns 401`() {
            mockMvc.get("/api/v1/tenants") {
                header("Authorization", userToken)   // missing "Bearer " prefix
            }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `tampered token signature returns 401`() {
            val tampered = userToken.dropLast(8) + "XXXXXXXX"

            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $tampered")
            }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `token signed with wrong secret returns 401`() {
            // Create a JwtUtil with a DIFFERENT secret to produce an invalid-signature token
            val rogueJwtUtil = JwtUtil(
                "completely-different-secret-that-is-long-enough",
                3600000L
            )
            val user = userRepository.findByEmail(userEmail).get()
            val rogueToken = rogueJwtUtil.generateToken(user, user.role.name)

            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $rogueToken")
            }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `expired token returns 401`() {
            // JwtUtil with -1ms expiry produces an already-expired token
            val expiredJwtUtil = JwtUtil(
                "h2-test-jwt-secret-exactly-32chars-long!",
                -1L
            )
            val user = userRepository.findByEmail(userEmail).get()
            val expiredToken = expiredJwtUtil.generateToken(user, user.role.name)

            Thread.sleep(10)   // ensure expiry has elapsed

            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $expiredToken")
            }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `valid USER token allows access to protected endpoint`() {
            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $userToken")
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
        }

        @Test
        fun `valid ADMIN token allows access to protected endpoint`() {
            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $adminToken")
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
        }

        @Test
        fun `public endpoints are accessible without any token`() {
            mockMvc.get("/actuator/health")
                .andExpect { status { isOk() } }

            // Swagger UI redirects to its index — either 200 or 302 is acceptable.
            // MockMvc does not follow redirects, so we accept either.
            val swaggerStatus = mockMvc.get("/swagger-ui.html").andReturn().response.status
            assert(swaggerStatus == 200 || swaggerStatus == 302) {
                "Expected swagger-ui.html to return 200 or 302 but got $swaggerStatus"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHORIZATION — Role-based access control
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class Authorization {

        // ── USER role ─────────────────────────────────────────────────────────

        @Test
        fun `USER can read tenants list (GET tenants)`() {
            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $userToken")
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
            }
        }

        @Test
        fun `USER cannot create a tenant and receives 403 Forbidden`() {
            val tenantPayload = mapOf(
                "name" to "Forbidden Tenant",
                "slug" to "forbidden-tenant",
                "contactEmail" to "forbidden@tenant.com"
            )

            mockMvc.post("/api/v1/tenants") {
                header("Authorization", "Bearer $userToken")
                contentType = MediaType.APPLICATION_JSON
                content = json(tenantPayload)
            }.andExpect {
                status { isForbidden() }
            }
        }

        @Test
        fun `USER cannot delete a tenant and receives 403 Forbidden`() {
            mockMvc.delete("/api/v1/tenants/00000000-0000-0000-0000-000000000001") {
                header("Authorization", "Bearer $userToken")
            }.andExpect {
                status { isForbidden() }
            }
        }

        // ── ADMIN role ────────────────────────────────────────────────────────

        @Test
        fun `ADMIN can read tenants list (GET tenants)`() {
            mockMvc.get("/api/v1/tenants") {
                header("Authorization", "Bearer $adminToken")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `ADMIN can create a tenant (POST tenants) and receives 201`() {
            val tenantPayload = mapOf(
                "name" to "Admin Created Tenant",
                "slug" to "admin-created-tenant",
                "contactEmail" to "admin-tenant@test.com"
            )

            mockMvc.post("/api/v1/tenants") {
                header("Authorization", "Bearer $adminToken")
                contentType = MediaType.APPLICATION_JSON
                content = json(tenantPayload)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.name") { value("Admin Created Tenant") }
            }
        }

        @Test
        fun `ADMIN can delete a tenant (DELETE tenants)`() {
            // First create a tenant to delete
            val createPayload = mapOf(
                "name" to "Tenant To Delete",
                "slug" to "tenant-to-delete",
                "contactEmail" to "delete@tenant.com"
            )
            val createResult = mockMvc.post("/api/v1/tenants") {
                header("Authorization", "Bearer $adminToken")
                contentType = MediaType.APPLICATION_JSON
                content = json(createPayload)
            }.andReturn()

            val tenantId = objectMapper
                .readTree(createResult.response.contentAsString)["data"]["id"].asText()

            mockMvc.delete("/api/v1/tenants/$tenantId") {
                header("Authorization", "Bearer $adminToken")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `unauthenticated request to admin endpoint returns 401 not 403`() {
            // Without any token, the response must be 401 (not authenticated), not 403 (not authorized).
            // This verifies that the entrypoint and access-denied handler are correctly wired.
            mockMvc.post("/api/v1/tenants") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "x", "slug" to "x", "contactEmail" to "x@x.com"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `USER GET tenants by unknown ID returns 404 not 403`() {
            // A USER can reach the endpoint — they just get a 404 for missing data.
            mockMvc.get("/api/v1/tenants/00000000-0000-0000-0000-000000000099") {
                header("Authorization", "Bearer $userToken")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Not Found") }
            }
        }

        @Test
        fun `auth endpoints remain accessible without any token (permit all)`() {
            mockMvc.post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("email" to "x@x.com", "password" to "wrong"))
            }.andExpect {
                // 401 from bad credentials — NOT 401 from missing token (reaches the controller)
                status { isUnauthorized() }
            }
        }
    }
}
