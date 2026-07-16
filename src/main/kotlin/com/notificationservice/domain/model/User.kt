package com.notificationservice.domain.model

import com.notificationservice.domain.enums.Role
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Application user entity.
 *
 * Implements [UserDetails] so it can be returned directly from
 * [com.notificationservice.service.UserDetailsServiceImpl] without an adapter.
 *
 * Password is stored as a BCrypt hash — never plain-text.
 * Role is stored as a VARCHAR mapped to the [Role] enum.
 */
@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_users_email", columnList = "email")]
)
class User(

    @Column(name = "email", nullable = false, unique = true, length = 320)
    val email: String,

    @Column(name = "password_hash", nullable = false, length = 60)
    private val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: Role = Role.USER,

    @Column(name = "enabled", nullable = false)
    private val enabled: Boolean = true,

) : BaseEntity(), UserDetails {

    // ── UserDetails implementation ────────────────────────────────────────────

    /**
     * Returns a single [SimpleGrantedAuthority] with prefix "ROLE_".
     * e.g. Role.ADMIN → "ROLE_ADMIN"
     */
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    /** Returns the BCrypt-hashed password. */
    override fun getPassword(): String = passwordHash

    /** Returns the email address as the username principal. */
    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = enabled
}
