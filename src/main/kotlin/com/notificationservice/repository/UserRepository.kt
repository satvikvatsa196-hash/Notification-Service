package com.notificationservice.repository

import com.notificationservice.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

/**
 * Spring Data JPA repository for [User] entities.
 */
interface UserRepository : JpaRepository<User, UUID> {

    /** Finds a user by their email address (case-sensitive). */
    fun findByEmail(email: String): Optional<User>

    /** Returns true if any user exists with the given email. */
    fun existsByEmail(email: String): Boolean
}
