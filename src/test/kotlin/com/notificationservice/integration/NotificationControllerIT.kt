package com.notificationservice.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.notificationservice.domain.enums.ChannelType
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
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationControllerIT {

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
            email = "notif-test-admin@example.com",
            passwordHash = passwordEncoder.encode("adminpass123"),
            role = Role.ADMIN
        )
        val savedAdmin = userRepository.save(admin)
        adminToken = jwtUtil.generateToken(savedAdmin, savedAdmin.role.name)

        val tenant = Tenant(name = "Notif Tenant", slug = "notif-tenant", contactEmail = "notif@example.com")
        testTenant = tenantRepository.save(tenant)
        
        val channel = Channel(tenant = testTenant, channelType = ChannelType.EMAIL, name = "Notif Channel", config = emptyMap())
        testChannel = channelRepository.save(channel)
    }

    @Test
    fun `POST create notification should return 201`() {
        val request = CreateNotificationRequest(
            channelId = testChannel.id!!,
            recipient = "user@example.com",
            subject = "Test Notification",
            content = "This is a test"
        )

        mockMvc.post("/api/v1/tenants/${testTenant.id}/notifications") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.recipient") { value(request.recipient) }
            jsonPath("$.data.status") { value("CREATED") }
        }
    }
}
