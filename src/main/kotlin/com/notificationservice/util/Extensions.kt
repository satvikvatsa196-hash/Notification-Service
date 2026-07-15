package com.notificationservice.util

import com.notificationservice.domain.model.Channel
import com.notificationservice.domain.model.Tenant
import com.notificationservice.dto.response.ChannelResponse
import com.notificationservice.dto.response.PageMeta
import com.notificationservice.dto.response.TenantResponse
import org.springframework.data.domain.Page

// ── Tenant mappings ──────────────────────────────────────────────────────────

fun Tenant.toResponse(): TenantResponse = TenantResponse(
    id = this.id!!,
    name = this.name,
    slug = this.slug,
    contactEmail = this.contactEmail,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

// ── Channel mappings ─────────────────────────────────────────────────────────

fun Channel.toResponse(): ChannelResponse = ChannelResponse(
    id = this.id!!,
    tenantId = this.tenant.id!!,
    channelType = this.channelType,
    name = this.name,
    config = this.config,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

// ── Pagination ───────────────────────────────────────────────────────────────

fun <T> Page<T>.toPageMeta(): PageMeta = PageMeta(
    page = this.number,
    size = this.size,
    totalElements = this.totalElements,
    totalPages = this.totalPages,
    isFirst = this.isFirst,
    isLast = this.isLast
)
