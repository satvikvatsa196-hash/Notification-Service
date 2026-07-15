package com.notificationservice.domain.model

import com.notificationservice.domain.enums.ChannelType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * A communication channel (Email, SMS, Push, etc.) belonging to a Tenant.
 * Channel-specific configuration (credentials, endpoints) is stored as JSONB.
 */
@Entity
@Table(
    name = "channels",
    indexes = [
        Index(name = "idx_channels_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_channels_type", columnList = "channel_type"),
        Index(name = "idx_channels_active", columnList = "active")
    ]
)
class Channel(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = ForeignKey(name = "fk_channels_tenant"))
    var tenant: Tenant,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 50)
    var channelType: ChannelType,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    /**
     * Flexible JSONB column holding channel-specific configuration.
     * e.g. for EMAIL: { "smtpHost": "smtp.example.com", "smtpPort": 587 }
     * e.g. for WEBHOOK: { "url": "https://...", "secret": "..." }
     * Uses Hibernate 6 native JSONB type mapping via @JdbcTypeCode(SqlTypes.JSON).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    var config: Map<String, Any> = emptyMap(),

    @Column(name = "active", nullable = false)
    var active: Boolean = true

) : BaseEntity() {

    @OneToMany(
        mappedBy = "channel",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        fetch = FetchType.LAZY
    )
    var templates: MutableList<NotificationTemplate> = mutableListOf()
}
