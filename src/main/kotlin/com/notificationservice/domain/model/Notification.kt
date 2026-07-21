package com.notificationservice.domain.model

import com.notificationservice.domain.enums.NotificationStatus
import jakarta.persistence.*
import java.time.Instant

/**
 * Represents an individual notification message to be sent or already sent.
 */
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_notifications_status", columnList = "status"),
        Index(name = "idx_notifications_created_at", columnList = "created_at")
    ]
)
class Notification(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = ForeignKey(name = "fk_notifications_tenant"))
    var tenant: Tenant,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false, foreignKey = ForeignKey(name = "fk_notifications_channel"))
    var channel: Channel,

    @Column(name = "recipient", nullable = false, length = 255)
    var recipient: String,

    @Column(name = "subject", length = 255)
    var subject: String? = null,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: NotificationStatus = NotificationStatus.CREATED,

    @Column(name = "scheduled_at")
    var scheduledAt: Instant? = null,

    @Column(name = "sent_at")
    var sentAt: Instant? = null,

    @Column(name = "error_details", columnDefinition = "TEXT")
    var errorDetails: String? = null,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

) : BaseEntity()
