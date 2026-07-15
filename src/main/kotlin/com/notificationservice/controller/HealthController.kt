package com.notificationservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Simple health check endpoint at /health (outside actuator) for load balancer probes.
 * Returns 200 OK when the application is up. Delegates detailed health to /actuator/health.
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Application health and readiness probes")
class HealthController(
    private val healthEndpoint: HealthEndpoint
) {

    @GetMapping
    @Operation(
        summary = "Simple liveness probe",
        description = "Returns 200 OK if the application is running. Suitable for load balancer health checks."
    )
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "notification-service"
            )
        )
    }
}
