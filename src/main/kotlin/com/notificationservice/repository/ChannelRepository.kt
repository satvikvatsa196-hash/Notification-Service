package com.notificationservice.repository

import com.notificationservice.domain.enums.ChannelType
import com.notificationservice.domain.model.Channel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ChannelRepository : JpaRepository<Channel, UUID> {

    fun findAllByTenantId(tenantId: UUID, pageable: Pageable): Page<Channel>

    fun findAllByTenantIdAndActive(tenantId: UUID, active: Boolean, pageable: Pageable): Page<Channel>

    fun findAllByTenantIdAndChannelType(tenantId: UUID, channelType: ChannelType, pageable: Pageable): Page<Channel>

    fun existsByTenantIdAndNameAndChannelType(tenantId: UUID, name: String, channelType: ChannelType): Boolean
}
