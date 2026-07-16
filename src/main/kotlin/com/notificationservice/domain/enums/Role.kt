package com.notificationservice.domain.enums

/**
 * Application roles for Role-Based Access Control (RBAC).
 *
 * Spring Security expects role names to be stored/used as-is here.
 * The `ROLE_` prefix is added automatically by Spring Security when using
 * [org.springframework.security.core.authority.SimpleGrantedAuthority] via
 * [org.springframework.security.core.userdetails.User.withUsername].
 */
enum class Role {
    USER,
    ADMIN
}
