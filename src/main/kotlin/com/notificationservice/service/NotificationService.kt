package com.notificationservice.service

import com.notificationservice.domain.enums.NotificationStatus
import com.notificationservice.domain.model.Notification
import com.notificationservice.dto.request.CreateNotificationRequest
import com.notificationservice.dto.request.NotificationFilter
import com.notificationservice.dto.response.NotificationResponse
import com.notificationservice.dto.response.PageMeta
import com.notificationservice.exception.ResourceNotFoundException
import com.notificationservice.repository.ChannelRepository
import com.notificationservice.repository.NotificationRepository
import com.notificationservice.repository.TenantRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.Predicate
import java.time.LocalDateTime
import java.util.UUID
import com.notificationservice.messaging.producer.NotificationProducer
import com.notificationservice.dto.event.NotificationEvent

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val tenantRepository: TenantRepository,
    private val channelRepository: ChannelRepository,
    private val notificationProducer: NotificationProducer
) {

    fun createNotification(tenantId: UUID, request: CreateNotificationRequest): NotificationResponse {
        val tenant = tenantRepository.findById(tenantId)
            .orElseThrow { ResourceNotFoundException("Tenant not found with ID: $tenantId") }

        val channel = channelRepository.findById(request.channelId)
            .orElseThrow { ResourceNotFoundException("Channel not found with ID: ${request.channelId}") }

        if (channel.tenant.id != tenantId) {
            throw ResourceNotFoundException("Channel not found for this tenant")
        }

        val notification = Notification(
            tenant = tenant,
            channel = channel,
            recipient = request.recipient,
            subject = request.subject,
            content = request.content,
            status = NotificationStatus.PENDING,
            scheduledAt = request.scheduledAt
        )

        val savedNotification = notificationRepository.save(notification)
        
        notificationProducer.sendNotificationEvent(
            NotificationEvent(
                notificationId = savedNotification.id!!,
                tenantId = tenantId
            )
        )

        return mapToResponse(savedNotification)
    }

    @Transactional(readOnly = true)
    fun getNotificationById(tenantId: UUID, notificationId: UUID): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ResourceNotFoundException("Notification not found with ID: $notificationId") }

        if (notification.tenant.id != tenantId) {
            throw ResourceNotFoundException("Notification not found for this tenant")
        }

        return mapToResponse(notification)
    }

    @Transactional(readOnly = true)
    fun listNotifications(
        tenantId: UUID,
        filter: NotificationFilter,
        pageable: Pageable
    ): Pair<List<NotificationResponse>, PageMeta> {
        val spec = Specification<Notification> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            
            // Filter by tenantId
            predicates.add(cb.equal(root.get<Any>("tenant").get<Any>("id"), tenantId))

            // Apply optional filters
            filter.status?.let {
                predicates.add(cb.equal(root.get<NotificationStatus>("status"), it))
            }
            filter.channelId?.let {
                predicates.add(cb.equal(root.get<Any>("channel").get<Any>("id"), it))
            }
            filter.startDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it))
            }
            filter.endDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it))
            }

            cb.and(*predicates.toTypedArray())
        }

        val page = notificationRepository.findAll(spec, pageable)
        val responses = page.content.map { mapToResponse(it) }

        val meta = PageMeta(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isFirst = page.isFirst,
            isLast = page.isLast
        )

        return Pair(responses, meta)
    }

    private fun mapToResponse(notification: Notification): NotificationResponse {
        return NotificationResponse(
            id = notification.id!!,
            tenantId = notification.tenant.id!!,
            channelId = notification.channel.id!!,
            recipient = notification.recipient,
            subject = notification.subject,
            content = notification.content,
            status = notification.status,
            scheduledAt = notification.scheduledAt,
            sentAt = notification.sentAt,
            errorDetails = notification.errorDetails,
            createdAt = notification.createdAt,
            updatedAt = notification.updatedAt
        )
    }

    @Transactional
    fun processNotification(notificationId: UUID) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ResourceNotFoundException("Notification not found with ID: $notificationId") }

        try {
            // Update status to PROCESSING
            notification.status = NotificationStatus.PROCESSING
            notificationRepository.saveAndFlush(notification)

            // Simulate actual sending logic (e.g., calling an external API)
            // Thread.sleep(500) 

            // Update status to SENT
            notification.status = NotificationStatus.SENT
            notification.sentAt = LocalDateTime.now()
            notificationRepository.save(notification)
        } catch (e: Exception) {
            notification.status = NotificationStatus.FAILED
            notification.errorDetails = e.message ?: "Unknown error occurred during processing"
            notificationRepository.save(notification)
            throw e
        }
    }
}
