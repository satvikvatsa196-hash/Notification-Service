package com.notificationservice.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.notificationservice.dto.request.LoginRequest
import com.notificationservice.dto.request.RegisterRequest
import com.notificationservice.domain.enums.Role
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration tests for authentication endpoints and protected resource access.
 *
 * Verifies:
 *  - Registration returns 201 and a valid JWT
 *  - Duplicate registration returns 409
 *  - Login returns 200 and a valid JWT
 *  - Bad credentials return 401
 *  - Protected endpoints are inaccessible without a token (401)
 *  - Authenticated USER can access non-admin endpoints
 *  - Authenticated USER cannot access ADMIN-only endpoints (403)
 *  - ADMIN user can access admin endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AuthControllerIT {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("notification_auth_test_db")
            withUsername("test_user")
            withPassword("test_pass")
        }

        // Shared token across tests (populated after registration)
        var userToken: String = ""
        var adminToken: String = ""
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    @Order(1)
    fun `POST auth register should create a USER and return 201 with token`() {
        val request = RegisterRequest(
            email = "user@integtest.com",
            password = "securepass123"
        )

        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.token") { isString() }
            jsonPath("$.data.email") { value("user@integtest.com") }
            jsonPath("$.data.role") { value("USER") }
        }.andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        userToken = body["data"]["token"].asText()
    }

    @Test
    @Order(2)
    fun `POST auth register should create an ADMIN and return 201`() {
        val request = RegisterRequest(
            email = "admin@integtest.com",
            password = "adminpass123",
            role = Role.ADMIN
        )

        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.role") { value("ADMIN") }
        }.andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        adminToken = body["data"]["token"].asText()
    }

    @Test
    @Order(3)
    fun `POST auth register should return 409 when email is already taken`() {
        val request = RegisterRequest(
            email = "user@integtest.com",   // same as in @Order(1)
            password = "anotherpassword"
        )

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error") { value("Conflict") }
        }
    }

    @Test
    @Order(4)
    fun `POST auth register should return 422 on blank password`() {
        val payload = mapOf("email" to "new@test.com", "password" to "")

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isUnprocessableEntity() }
            jsonPath("$.error") { value("Validation Failed") }
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    fun `POST auth login should return 200 with token for valid credentials`() {
        val request = LoginRequest(
            email = "user@integtest.com",
            password = "securepass123"
        )

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.token") { isString() }
            jsonPath("$.data.email") { value("user@integtest.com") }
            jsonPath("$.data.role") { value("USER") }
        }
    }

    @Test
    @Order(6)
    fun `POST auth login should return 401 for wrong password`() {
        val request = LoginRequest(
            email = "user@integtest.com",
            password = "wrongpassword"
        )

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @Order(7)
    fun `POST auth login should return 401 for unknown email`() {
        val request = LoginRequest(
            email = "ghost@nobody.com",
            password = "anything"
        )

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ── Protected endpoint access ─────────────────────────────────────────────

    @Test
    @Order(8)
    fun `GET tenants should return 401 without a token`() {
        mockMvc.get("/api/v1/tenants")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    @Order(9)
    fun `GET tenants should return 200 for authenticated USER`() {
        mockMvc.get("/api/v1/tenants") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    @Order(10)
    fun `POST tenants should return 403 for authenticated USER (admin only)`() {
        val payload = mapOf(
            "name" to "Test Tenant",
            "slug" to "test-tenant",
            "contactEmail" to "tenant@test.com"
        )

        mockMvc.post("/api/v1/tenants") {
            header("Authorization", "Bearer $userToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @Order(11)
    fun `POST tenants should return 201 for authenticated ADMIN`() {
        val payload = mapOf(
            "name" to "Admin Created Tenant ${System.currentTimeMillis()}",
            "slug" to "admin-tenant-${System.currentTimeMillis()}",
            "contactEmail" to "tenant@admin.com"
        )

        mockMvc.post("/api/v1/tenants") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    @Order(12)
    fun `GET tenants should return 401 for invalid Bearer token`() {
        mockMvc.get("/api/v1/tenants") {
            header("Authorization", "Bearer this.is.garbage")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
