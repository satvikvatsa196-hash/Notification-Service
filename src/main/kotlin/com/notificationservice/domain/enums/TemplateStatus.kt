package com.notificationservice.domain.enums

/**
 * Lifecycle status for a NotificationTemplate.
 *
 * Transitions:
 *   DRAFT → ACTIVE   (publish / activate)
 *   ACTIVE → ARCHIVED (retire)
 *   ARCHIVED → DRAFT  (restore for editing)
 */
enum class TemplateStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
