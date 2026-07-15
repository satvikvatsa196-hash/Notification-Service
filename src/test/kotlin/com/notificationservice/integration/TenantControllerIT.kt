package com.notificationservice.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.notificationservice.dto.request.CreateTenantRequest
import org.junit.jupiter.api.Test
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
 * Integration test for the Tenant API.
 *
 * Uses Testcontainers to spin up a real PostgreSQL instance.
 * @ServiceConnection auto-configures the datasource URL — no manual property overriding needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TenantControllerIT {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("notification_test_db")
            withUsername("test_user")
            withPassword("test_pass")
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `GET tenants should return 200 with empty list initially`() {
        mockMvc.get("/api/v1/tenants")
            .andExpect {
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
            "name" to "",   // blank name — should fail validation
            "slug" to "valid-slug",
            "contactEmail" to "not-an-email"   // invalid email
        )

        mockMvc.post("/api/v1/tenants") {
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
        mockMvc.get("/api/v1/tenants/00000000-0000-0000-0000-000000000099")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Not Found") }
            }
    }
}
