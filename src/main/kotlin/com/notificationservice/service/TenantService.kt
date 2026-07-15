package com.notificationservice.service

import com.notificationservice.domain.model.Tenant
import com.notificationservice.dto.request.CreateTenantRequest
import com.notificationservice.dto.response.PageMeta
import com.notificationservice.dto.response.TenantResponse
import com.notificationservice.exception.DuplicateResourceException
import com.notificationservice.exception.ResourceNotFoundException
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
class TenantService(
    private val tenantRepository: TenantRepository
) {

    private val log = LoggerFactory.getLogger(TenantService::class.java)

    /**
     * Retrieves a paginated list of all active tenants.
     */
    fun listTenants(pageable: Pageable): Pair<List<TenantResponse>, PageMeta> {
        val page = tenantRepository.findAllActive(pageable)
        return Pair(page.content.map { it.toResponse() }, page.toPageMeta())
    }

    /**
     * Retrieves a single tenant by its UUID.
     * @throws ResourceNotFoundException if the tenant does not exist
     */
    fun getTenantById(id: UUID): TenantResponse {
        val tenant = findTenantOrThrow(id)
        return tenant.toResponse()
    }

    /**
     * Creates a new tenant after validating uniqueness of name and slug.
     * @throws DuplicateResourceException if name or slug is already taken
     */
    @Transactional
    fun createTenant(request: CreateTenantRequest): TenantResponse {
        log.info("Creating tenant: name='{}', slug='{}'", request.name, request.slug)

        if (tenantRepository.existsByName(request.name)) {
            throw DuplicateResourceException("Tenant", "name", request.name)
        }
        if (tenantRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("Tenant", "slug", request.slug)
        }

        val tenant = Tenant(
            name = request.name,
            slug = request.slug,
            contactEmail = request.contactEmail
        )

        val saved = tenantRepository.save(tenant)
        log.info("Tenant created successfully: id='{}'", saved.id)

        return saved.toResponse()
    }

    /**
     * Soft-deletes a tenant by setting active = false.
     * @throws ResourceNotFoundException if the tenant does not exist
     */
    @Transactional
    fun deactivateTenant(id: UUID) {
        val tenant = findTenantOrThrow(id)
        tenant.active = false
        tenantRepository.save(tenant)
        log.info("Tenant deactivated: id='{}'", id)
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun findTenantOrThrow(id: UUID): Tenant =
        tenantRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Tenant", id) }
}
