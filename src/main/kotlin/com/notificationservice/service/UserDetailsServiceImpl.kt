package com.notificationservice.service

import com.notificationservice.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Spring Security hook that loads a [com.notificationservice.domain.model.User]
 * by email during authentication.
 *
 * The [com.notificationservice.domain.model.User] entity itself implements
 * [UserDetails], so no adapter is needed.
 */
@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    /**
     * Loads a user by email.
     *
     * @throws UsernameNotFoundException if no user with the given email exists.
     */
    override fun loadUserByUsername(email: String): UserDetails =
        userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("No user found with email: $email") }
}
