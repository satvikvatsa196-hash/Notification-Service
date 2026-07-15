package com.notificationservice.repository

import com.notificationservice.domain.enums.TemplateStatus
import com.notificationservice.domain.model.NotificationTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface NotificationTemplateRepository : JpaRepository<NotificationTemplate, UUID> {

    fun findByTenantIdAndName(tenantId: UUID, name: String): Optional<NotificationTemplate>

    fun findAllByTenantId(tenantId: UUID, pageable: Pageable): Page<NotificationTemplate>

    fun findAllByTenantIdAndStatus(tenantId: UUID, status: TemplateStatus, pageable: Pageable): Page<NotificationTemplate>

    fun findAllByTenantIdAndChannelId(tenantId: UUID, channelId: UUID, pageable: Pageable): Page<NotificationTemplate>

    fun existsByTenantIdAndName(tenantId: UUID, name: String): Boolean
}
