package com.notificationservice.service

import com.notificationservice.domain.model.Channel
import com.notificationservice.dto.request.CreateChannelRequest
import com.notificationservice.dto.response.ChannelResponse
import com.notificationservice.dto.response.PageMeta
import com.notificationservice.exception.DuplicateResourceException
import com.notificationservice.exception.ResourceNotFoundException
import com.notificationservice.repository.ChannelRepository
import com.notificationservice.repository.TenantRepository
import com.notificationservice.util.toResponse
import com.notificationservice.util.toPageMeta
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ChannelService(
    private val channelRepository: ChannelRepository,
    private val tenantRepository: TenantRepository
) {

    private val log = LoggerFactory.getLogger(ChannelService::class.java)

    /**
     * Lists all channels belonging to a tenant (paginated).
     */
    fun listChannelsForTenant(tenantId: UUID, pageable: Pageable): Pair<List<ChannelResponse>, PageMeta> {
        ensureTenantExists(tenantId)
        val page = channelRepository.findAllByTenantId(tenantId, pageable)
        return Pair(page.content.map { it.toResponse() }, page.toPageMeta())
    }

    /**
     * Creates a new channel under a tenant.
     * @throws ResourceNotFoundException if the tenant does not exist
     * @throws DuplicateResourceException if a channel with the same name and type already exists
     */
    @Transactional
    fun createChannel(tenantId: UUID, request: CreateChannelRequest): ChannelResponse {
        log.info("Creating channel: tenantId='{}', type='{}', name='{}'", tenantId, request.channelType, request.name)

        val tenant = tenantRepository.findById(tenantId)
            .orElseThrow { ResourceNotFoundException("Tenant", tenantId) }

        if (channelRepository.existsByTenantIdAndNameAndChannelType(tenantId, request.name, request.channelType)) {
            throw DuplicateResourceException("Channel", "name + channelType", "${request.name} (${request.channelType})")
        }

        val channel = Channel(
            tenant = tenant,
            channelType = request.channelType,
            name = request.name,
            config = request.config
        )

        val saved = channelRepository.save(channel)
        log.info("Channel created: id='{}'", saved.id)

        return saved.toResponse()
    }

    /**
     * Retrieves a single channel by ID, scoped to the given tenant.
     */
    fun getChannelById(tenantId: UUID, channelId: UUID): ChannelResponse {
        ensureTenantExists(tenantId)
        val channel = channelRepository.findById(channelId)
            .filter { it.tenant.id == tenantId }
            .orElseThrow { ResourceNotFoundException("Channel", channelId) }
        return channel.toResponse()
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun ensureTenantExists(tenantId: UUID) {
        if (!tenantRepository.existsById(tenantId)) {
            throw ResourceNotFoundException("Tenant", tenantId)
        }
    }
}
