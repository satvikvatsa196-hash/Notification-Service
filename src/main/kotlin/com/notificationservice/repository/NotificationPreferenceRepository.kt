package com.notificationservice.repository

import com.notificationservice.domain.model.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, UUID> {
    fun findByTenantIdAndRecipient(tenantId: UUID, recipient: String): List<NotificationPreference>
}
