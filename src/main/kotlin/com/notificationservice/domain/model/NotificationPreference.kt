package com.notificationservice.domain.model

import com.notificationservice.domain.enums.ChannelType
import jakarta.persistence.*

@Entity
@Table(
    name = "notification_preferences",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_preferences_recipient_channel", columnNames = ["tenant_id", "recipient", "channel_type"])
    ],
    indexes = [
        Index(name = "idx_preferences_recipient", columnList = "tenant_id, recipient")
    ]
)
class NotificationPreference(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = ForeignKey(name = "fk_preferences_tenant"))
    var tenant: Tenant,

    @Column(name = "recipient", nullable = false, length = 255)
    var recipient: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 50)
    var channelType: ChannelType,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true
) : BaseEntity()
