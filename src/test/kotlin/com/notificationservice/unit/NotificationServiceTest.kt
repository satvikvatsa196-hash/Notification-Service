package com.notificationservice.unit

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.enums.NotificationStatus
import com.notificationservice.domain.model.Channel
import com.notificationservice.domain.model.Notification
import com.notificationservice.domain.model.Tenant
import com.notificationservice.dto.request.CreateNotificationRequest
import com.notificationservice.repository.ChannelRepository
import com.notificationservice.repository.NotificationRepository
import com.notificationservice.repository.TenantRepository
import com.notificationservice.service.NotificationService
import com.notificationservice.messaging.producer.NotificationProducer
import com.notificationservice.dto.event.NotificationEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    @Mock
    private lateinit var tenantRepository: TenantRepository

    @Mock
    private lateinit var channelRepository: ChannelRepository

    @Mock
    private lateinit var notificationProducer: NotificationProducer

    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationService = NotificationService(notificationRepository, tenantRepository, channelRepository, notificationProducer)
    }

    @Test
    fun `createNotification should save and return notification response`() {
        val tenantId = UUID.randomUUID()
        val channelId = UUID.randomUUID()
        val tenant = Tenant(name = "Test Tenant", slug = "test", contactEmail = "test@example.com").apply { id = tenantId }
        val channel = Channel(tenant = tenant, channelType = ChannelType.EMAIL, name = "Test Channel", config = emptyMap()).apply { id = channelId }
        
        val request = CreateNotificationRequest(
            channelId = channelId,
            recipient = "user@example.com",
            subject = "Test",
            content = "Hello World"
        )

        `when`(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant))
        `when`(channelRepository.findById(channelId)).thenReturn(Optional.of(channel))
        `when`(notificationRepository.save(any(Notification::class.java))).thenAnswer { 
            val notif = it.arguments[0] as Notification
            notif.id = UUID.randomUUID()
            notif.createdAt = Instant.now()
            notif.updatedAt = Instant.now()
            notif
        }

        val response = notificationService.createNotification(tenantId, request)

        assertNotNull(response)
        assertEquals("user@example.com", response.recipient)
        assertEquals(NotificationStatus.PENDING, response.status)
        verify(notificationRepository, times(1)).save(any(Notification::class.java))
        verify(notificationProducer, times(1)).sendNotificationEvent(any(NotificationEvent::class.java))
    }
}
