package com.notificationservice.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.notificationservice.domain.enums.Role
import com.notificationservice.dto.request.CreateTenantRequest
import com.notificationservice.dto.request.RegisterRequest
import com.notificationservice.repository.UserRepository
import com.notificationservice.util.JwtUtil
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * Integration test for the Tenant API.
 *
 * Uses Testcontainers to spin up a real PostgreSQL instance.
 * @ServiceConnection auto-configures the datasource URL — no manual property overriding needed.
 *
 * Since endpoints are now secured, we register an ADMIN user in @BeforeAll
 * and attach the token to all requests that require authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantControllerIT {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtUtil: JwtUtil

    private var adminToken: String = ""

    @BeforeAll
    fun setUpAuth() {
        // Wipe all users first
        userRepository.deleteAll()

        // Create an ADMIN directly in the test database
        val admin = com.notificationservice.domain.model.User(
            email = "tenant-test-admin@example.com",
            passwordHash = passwordEncoder.encode("adminpass123"),
            role = Role.ADMIN
        )
        val savedAdmin = userRepository.save(admin)
        adminToken = jwtUtil.generateToken(savedAdmin, savedAdmin.role.name)
    }

    @Test
    fun `GET tenants should return 200 with empty list initially`() {
        mockMvc.get("/api/v1/tenants") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data") { isArray() }
        }
    }

    @Test
    fun `POST tenants should create a tenant and return 201`() {
        val request = CreateTenantRequest(
            name = "Test Tenant ${System.currentTimeMillis()}",
            slug = "test-tenant-${System.currentTimeMillis()}",
            contactEmail = "test@example.com"
        )

        mockMvc.post("/api/v1/tenants") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.name") { value(request.name) }
            jsonPath("$.data.slug") { value(request.slug) }
            jsonPath("$.data.active") { value(true) }
        }
    }

    @Test
    fun `POST tenants should return 422 when request body is invalid`() {
        val invalidRequest = mapOf(
            "name" to "",           // blank name — should fail validation
            "slug" to "valid-slug",
            "contactEmail" to "not-an-email"  // invalid email
        )

        mockMvc.post("/api/v1/tenants") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidRequest)
        }.andExpect {
            status { isUnprocessableEntity() }
            jsonPath("$.error") { value("Validation Failed") }
            jsonPath("$.details") { isArray() }
        }
    }

    @Test
    fun `GET tenants by unknown ID should return 404`() {
        mockMvc.get("/api/v1/tenants/00000000-0000-0000-0000-000000000099") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Not Found") }
        }
    }
}
