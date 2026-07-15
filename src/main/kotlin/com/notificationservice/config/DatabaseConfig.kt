package com.notificationservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.Optional

/**
 * JPA Auditing configuration.
 *
 * Provides the current "auditor" (user/system identity) for @CreatedBy / @LastModifiedBy.
 * Returns "system" until authentication is implemented; swap this bean with a
 * SecurityContextHolder-backed implementation when auth is added.
 */
@Configuration
class DatabaseConfig : AuditorAware<String> {

    override fun getCurrentAuditor(): Optional<String> {
        // TODO: Replace with SecurityContextHolder.getContext().authentication?.name when auth is added
        return Optional.of("system")
    }
}
