package com.notificationservice.exception

/**
 * Thrown when attempting to create a resource that already exists.
 * Maps to HTTP 409 Conflict.
 */
class DuplicateResourceException(message: String) : RuntimeException(message) {

    constructor(resourceType: String, field: String, value: String) :
        this("$resourceType with $field '$value' already exists")
}
