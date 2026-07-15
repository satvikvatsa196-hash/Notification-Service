package com.notificationservice.domain.model

import jakarta.persistence.*

/**
 * Represents an organization or application that uses the notification service.
 * Every other resource is scoped to a Tenant.
 */
@Entity
@Table(
    name = "tenants",
    indexes = [
        Index(name = "idx_tenants_slug", columnList = "slug"),
        Index(name = "idx_tenants_active", columnList = "active")
    ]
)
class Tenant(

    @Column(name = "name", nullable = false, unique = true, length = 255)
    var name: String,

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    var slug: String,

    @Column(name = "contact_email", nullable = false, length = 320)
    var contactEmail: String,

    @Column(name = "active", nullable = false)
    var active: Boolean = true

) : BaseEntity() {

    /**
     * Channels registered under this tenant.
     * Lazy-loaded to prevent unintended Cartesian product queries.
     */
    @OneToMany(
        mappedBy = "tenant",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var channels: MutableList<Channel> = mutableListOf()

    /**
     * Templates owned by this tenant.
     */
    @OneToMany(
        mappedBy = "tenant",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var templates: MutableList<NotificationTemplate> = mutableListOf()
}
