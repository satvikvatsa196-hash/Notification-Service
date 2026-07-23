package com.notificationservice.service

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.model.NotificationPreference
import com.notificationservice.repository.NotificationPreferenceRepository
import com.notificationservice.repository.TenantRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class NotificationPreferenceService(
    private val preferenceRepository: NotificationPreferenceRepository,
    private val tenantRepository: TenantRepository
) {
    @Cacheable(value = ["notificationPreferences"], key = "#tenantId.toString() + ':' + #recipient", unless = "#result == null")
    @Transactional(readOnly = true)
    fun getPreferences(tenantId: UUID, recipient: String): Map<ChannelType, Boolean> {
        val preferences = preferenceRepository.findByTenantIdAndRecipient(tenantId, recipient)
        return preferences.associate { it.channelType to it.enabled }
    }

    @CacheEvict(value = ["notificationPreferences"], key = "#tenantId.toString() + ':' + #recipient")
    fun updatePreference(tenantId: UUID, recipient: String, channelType: ChannelType, enabled: Boolean): NotificationPreference {
        val preferences = preferenceRepository.findByTenantIdAndRecipient(tenantId, recipient)
        var preference = preferences.find { it.channelType == channelType }

        if (preference != null) {
            preference.enabled = enabled
        } else {
            val tenant = tenantRepository.findById(tenantId)
                .orElseThrow { IllegalArgumentException("Tenant not found") }
            preference = NotificationPreference(
                tenant = tenant,
                recipient = recipient,
                channelType = channelType,
                enabled = enabled
            )
        }
        return preferenceRepository.save(preference)
    }
}
