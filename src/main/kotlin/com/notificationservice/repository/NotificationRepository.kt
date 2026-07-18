package com.notificationservice.repository

import com.notificationservice.domain.model.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {
}
