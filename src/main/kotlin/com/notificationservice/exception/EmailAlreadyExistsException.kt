package com.notificationservice.exception

/**
 * Thrown when registration is attempted with an email that already exists.
 */
class EmailAlreadyExistsException(email: String) :
    RuntimeException("A user with email '$email' already exists")
