package com.notificationservice.unit

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.model.NotificationPreference
import com.notificationservice.domain.model.Tenant
import com.notificationservice.repository.NotificationPreferenceRepository
import com.notificationservice.repository.TenantRepository
import com.notificationservice.service.NotificationPreferenceService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationPreferenceServiceTest {

    @Mock
    private lateinit var preferenceRepository: NotificationPreferenceRepository

    @Mock
    private lateinit var tenantRepository: TenantRepository

    private lateinit var preferenceService: NotificationPreferenceService

    @BeforeEach
    fun setUp() {
        preferenceService = NotificationPreferenceService(preferenceRepository, tenantRepository)
    }

    @Test
    fun `getPreferences should return mapped preferences for a user`() {
        val tenantId = UUID.randomUUID()
        val recipient = "user@example.com"
        val tenant = Tenant(name = "Test", slug = "test", contactEmail = "test@example.com").apply { id = tenantId }
        
        val pref1 = NotificationPreference(tenant, recipient, ChannelType.EMAIL, true)
        val pref2 = NotificationPreference(tenant, recipient, ChannelType.SMS, false)

        `when`(preferenceRepository.findByTenantIdAndRecipient(tenantId, recipient)).thenReturn(listOf(pref1, pref2))

        val result = preferenceService.getPreferences(tenantId, recipient)

        assertEquals(2, result.size)
        assertTrue(result[ChannelType.EMAIL] == true)
        assertFalse(result[ChannelType.SMS] == true)
    }

    @Test
    fun `updatePreference should update existing preference and invalidate cache`() {
        val tenantId = UUID.randomUUID()
        val recipient = "user@example.com"
        val tenant = Tenant(name = "Test", slug = "test", contactEmail = "test@example.com").apply { id = tenantId }
        
        val existingPref = NotificationPreference(tenant, recipient, ChannelType.EMAIL, true)
        `when`(preferenceRepository.findByTenantIdAndRecipient(tenantId, recipient)).thenReturn(listOf(existingPref))
        `when`(preferenceRepository.save(any(NotificationPreference::class.java))).thenAnswer { it.arguments[0] }

        val updatedPref = preferenceService.updatePreference(tenantId, recipient, ChannelType.EMAIL, false)

        assertFalse(updatedPref.enabled)
        verify(preferenceRepository, times(1)).save(existingPref)
    }
}
