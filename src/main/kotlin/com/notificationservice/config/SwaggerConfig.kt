package com.notificationservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI 3 / Swagger UI configuration.
 * Access at: http://localhost:8080/swagger-ui.html
 */
@Configuration
class SwaggerConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Notification Service API")
                .version("1.0.0")
                .description(
                    """
                    **Notification Service** — production-quality backend for managing multi-channel notifications.
                    
                    ## Features
                    - Multi-tenant architecture
                    - Support for Email, SMS, Push, Webhook, and In-App channels
                    - Template management with versioning
                    - Full audit trail on all resources
                    
                    ## Authentication
                    _Authentication is not yet implemented in this foundation release._
                    """.trimIndent()
                )
                .contact(
                    Contact()
                        .name("Notification Service Team")
                        .email("team@notificationservice.internal")
                )
                .license(
                    License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT")
                )
        )
        .components(Components())
        .servers(
            listOf(
                Server().url("http://localhost:8080").description("Local Development"),
                Server().url("https://api.notificationservice.internal").description("Production")
            )
        )
}
