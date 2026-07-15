package com.notificationservice.domain.enums

/**
 * Supported notification delivery channel types.
 * Stored as VARCHAR in the database (not ordinal) for human-readability and stability.
 */
enum class ChannelType {
    EMAIL,
    SMS,
    PUSH,
    WEBHOOK,
    IN_APP
}
