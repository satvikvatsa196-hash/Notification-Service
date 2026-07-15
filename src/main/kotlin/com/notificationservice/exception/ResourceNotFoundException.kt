package com.notificationservice.exception

import java.util.UUID

/**
 * Thrown when a requested resource cannot be found.
 * Maps to HTTP 404 Not Found.
 */
class ResourceNotFoundException(message: String) : RuntimeException(message) {

    constructor(resourceType: String, id: UUID) :
        this("$resourceType with id '$id' was not found")

    constructor(resourceType: String, field: String, value: String) :
        this("$resourceType with $field '$value' was not found")
}
