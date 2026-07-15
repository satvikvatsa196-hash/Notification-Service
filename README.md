# Notification Service

A multi-channel notification backend built with Kotlin, Spring Boot 3, RabbitMQ, and PostgreSQL, featuring asynchronous processing, retry mechanisms, scheduling, and user notification preferences.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 (JVM 21) |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Containers | Docker & Docker Compose |
| Testing | JUnit 5 + MockK + Testcontainers |
| Logging | Logback + Logstash JSON encoder |

---

## Quick Start

### 1. Start PostgreSQL via Docker Compose

```bash
cp .env.example .env
docker-compose up postgres -d
```

### 2. Run the Application (local profile)

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. Explore the API

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Health Check | http://localhost:8080/health |
| Actuator Health | http://localhost:8080/actuator/health |

---

## Running with Docker Compose (Full Stack)

```bash
docker-compose up --build
```

---

## Running Tests

```bash
# Unit tests only (fast, no Docker required)
./gradlew test --tests "*.unit.*"

# Integration tests (requires Docker for Testcontainers)
./gradlew test --tests "*.integration.*"

# All tests
./gradlew test
```

---

## API Overview

### Tenants

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/tenants` | List all active tenants (paginated) |
| `GET` | `/api/v1/tenants/{id}` | Get tenant by ID |
| `POST` | `/api/v1/tenants` | Create a new tenant |
| `DELETE` | `/api/v1/tenants/{id}` | Soft-delete a tenant |

### Channels

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/tenants/{tenantId}/channels` | List channels for a tenant |
| `GET` | `/api/v1/tenants/{tenantId}/channels/{id}` | Get channel by ID |
| `POST` | `/api/v1/tenants/{tenantId}/channels` | Create a channel |

---

## Project Structure

```
src/main/kotlin/com/notificationservice/
├── config/          # Spring configuration (Swagger, Database, AppProperties)
├── controller/      # REST controllers (request/response handling only)
├── domain/
│   ├── model/       # JPA entities (BaseEntity, Tenant, Channel, Template)
│   └── enums/       # ChannelType, TemplateStatus
├── dto/
│   ├── request/     # Validated incoming request DTOs
│   └── response/    # Outgoing response DTOs (ApiResponse envelope)
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── util/            # Kotlin extension functions (entity→DTO mapping)
```

---

## Error Response Format (RFC 7807)

All API errors return a consistent JSON envelope:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 422,
  "error": "Validation Failed",
  "message": "Request contains 2 validation error(s)",
  "path": "/api/v1/tenants",
  "details": [
    { "field": "name", "message": "Name must not be blank", "rejectedValue": "" },
    { "field": "contactEmail", "message": "Must be a valid email address", "rejectedValue": "not-email" }
  ]
}
```
