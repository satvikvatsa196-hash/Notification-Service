package com.notificationservice.repository

import com.notificationservice.domain.model.Tenant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface TenantRepository : JpaRepository<Tenant, UUID> {

    fun findBySlug(slug: String): Optional<Tenant>

    fun existsBySlug(slug: String): Boolean

    fun existsByName(name: String): Boolean

    fun findAllByActive(active: Boolean, pageable: Pageable): Page<Tenant>

    @Query("SELECT t FROM Tenant t WHERE t.active = true ORDER BY t.createdAt DESC")
    fun findAllActive(pageable: Pageable): Page<Tenant>
}
