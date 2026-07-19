package com.notificationservice.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.enums.NotificationStatus
import com.notificationservice.domain.enums.Role
import com.notificationservice.domain.model.Channel
import com.notificationservice.domain.model.Tenant
import com.notificationservice.domain.model.User
import com.notificationservice.dto.request.CreateNotificationRequest
import com.notificationservice.repository.ChannelRepository
import com.notificationservice.repository.NotificationRepository
import com.notificationservice.repository.TenantRepository
import com.notificationservice.repository.UserRepository
import com.notificationservice.util.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import org.awaitility.Awaitility.await

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class NotificationMessagingIT {

    companion object {
        @Container
        val rabbitMQContainer = RabbitMQContainer("rabbitmq:3-management")

        @JvmStatic
        @DynamicPropertySource
        fun rabbitProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.rabbitmq.host") { rabbitMQContainer.host }
            registry.add("spring.rabbitmq.port") { rabbitMQContainer.amqpPort }
            registry.add("spring.rabbitmq.username") { rabbitMQContainer.adminUsername }
            registry.add("spring.rabbitmq.password") { rabbitMQContainer.adminPassword }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository
    
    @Autowired
    lateinit var tenantRepository: TenantRepository
    
    @Autowired
    lateinit var channelRepository: ChannelRepository
    
    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtUtil: JwtUtil

    private var adminToken: String = ""
    private lateinit var testTenant: Tenant
    private lateinit var testChannel: Channel

    @BeforeAll
    fun setUpData() {
        notificationRepository.deleteAll()
        userRepository.deleteAll()
        channelRepository.deleteAll()
        tenantRepository.deleteAll()

        val admin = User(
            email = "rabbit-admin@example.com",
            passwordHash = passwordEncoder.encode("adminpass123"),
            role = Role.ADMIN
        )
        val savedAdmin = userRepository.save(admin)
        adminToken = jwtUtil.generateToken(savedAdmin, savedAdmin.role.name)

        val tenant = Tenant(name = "Rabbit Tenant", slug = "rabbit-tenant", contactEmail = "rabbit@example.com")
        testTenant = tenantRepository.save(tenant)
        
        val channel = Channel(tenant = testTenant, channelType = ChannelType.EMAIL, name = "Rabbit Channel", config = emptyMap())
        testChannel = channelRepository.save(channel)
    }

    @Test
    fun `creating a notification should send a message and worker should process it`() {
        val request = CreateNotificationRequest(
            channelId = testChannel.id!!,
            recipient = "consumer@example.com",
            subject = "Async Notification",
            content = "This should be processed asynchronously"
        )

        val result = mockMvc.post("/api/v1/tenants/${testTenant.id}/notifications") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.status") { value("PENDING") } // Since we changed it to PENDING initially
        }.andReturn()

        val responseContent = result.response.contentAsString
        val jsonNode = objectMapper.readTree(responseContent)
        val notificationId = jsonNode.get("data").get("id").asText()

        // Wait for the consumer to process the message and update the status to SENT
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val notification = notificationRepository.findById(java.util.UUID.fromString(notificationId)).orElseThrow()
            assertEquals(NotificationStatus.SENT, notification.status)
        }
    }
}
