package com.notificationservice.domain.model

import com.notificationservice.domain.enums.TemplateStatus
import jakarta.persistence.*

/**
 * A reusable message template tied to a Tenant and a Channel.
 * Body content supports placeholder variables (e.g. {{user_name}}).
 */
@Entity
@Table(
    name = "notification_templates",
    indexes = [
        Index(name = "idx_templates_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_templates_channel_id", columnList = "channel_id"),
        Index(name = "idx_templates_status", columnList = "status")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_templates_tenant_name", columnNames = ["tenant_id", "name"])
    ]
)
class NotificationTemplate(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = ForeignKey(name = "fk_templates_tenant"))
    var tenant: Tenant,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false, foreignKey = ForeignKey(name = "fk_templates_channel"))
    var channel: Channel,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "subject", length = 500)
    var subject: String? = null,

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    var bodyTemplate: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: TemplateStatus = TemplateStatus.DRAFT

) : BaseEntity()
