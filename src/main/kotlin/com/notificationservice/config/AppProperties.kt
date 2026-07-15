package com.notificationservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Strongly-typed binding of custom `app.*` properties from application.yml.
 * Use constructor injection anywhere via @Autowired or primary constructor.
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val api: ApiProperties = ApiProperties(),
    val cors: CorsProperties = CorsProperties()
) {
    data class ApiProperties(
        val version: String = "v1",
        val prefix: String = "/api/v1"
    )

    data class CorsProperties(
        val allowedOrigins: List<String> = emptyList()
    )
}
