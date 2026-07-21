# Notification Service

A Notification Service backend built with Kotlin, Spring Boot 3, PostgreSQL, and Spring Security. Currently includes JWT authentication, role-based authorization, Flyway migrations, Docker support, and a clean layered architecture. Features include Asynchronous notification processing with RabbitMQ, dynamic delivery providers via Strategy Pattern, comprehensive Notification Management, and robust failure handling (including automatic retries with exponential backoff and a Dead Letter Queue). Caching and scheduling will be added in subsequent milestones.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 (JVM 21) |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Messaging | RabbitMQ |
| Security | Spring Security 6 + JWT (JJWT 0.12) + BCrypt |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Containers | Docker & Docker Compose |
| Testing | JUnit 5 + MockK + Testcontainers |
| Logging | Logback + Logstash JSON encoder |

---

## Reliability & Failure Handling

To ensure maximum deliverability and stability when integrating with external channel providers, the service leverages RabbitMQ and specific error handling strategies:

- **Transient vs Permanent Failures**: Differentiates between temporary glitches (e.g., rate limits, network timeouts) via `TransientDeliveryException` and unrecoverable errors (e.g., malformed recipients) via `PermanentDeliveryException`.
- **Automatic Retries with Exponential Backoff**: Transient failures are automatically retried up to 3 times, with an exponentially increasing delay between attempts to avoid overwhelming downstream services.
- **Dead Letter Queue (DLQ)**: If a notification fails permanently or exhausts its maximum retry limit, it is automatically routed to a `notification.dlq` queue for later manual inspection and replay without blocking healthy traffic.

---

## Quick Start

### 1. Start PostgreSQL and RabbitMQ via Docker Compose

```bash
cp .env.example .env
docker-compose up postgres rabbitmq -d
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
| Health Check | http://localhost:8080/actuator/health |

---

## Authentication

The API uses **stateless JWT authentication**. Every protected endpoint requires a `Bearer` token in the `Authorization` header.

### Register a user

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "securepass123", "role": "USER"}'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "securepass123"}'
```

Both endpoints return:

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "role": "USER"
  }
}
```

### Using the token

```bash
curl http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Authorization & Roles

| Role | Description |
|---|---|
| `USER` | Read access to tenants and channels |
| `ADMIN` | Full access — can create and delete tenants |

### Endpoint protection

| Method | Endpoint | Required Role |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Public |
| `POST` | `/api/v1/auth/login` | Public |
| `GET` | `/api/v1/tenants` | USER or ADMIN |
| `GET` | `/api/v1/tenants/{id}` | USER or ADMIN |
| `POST` | `/api/v1/tenants` | ADMIN only |
| `DELETE` | `/api/v1/tenants/{id}` | ADMIN only |
| `GET` | `/api/v1/tenants/{id}/channels` | USER or ADMIN |
| `POST` | `/api/v1/tenants/{id}/channels` | USER or ADMIN |
| `GET` | `/api/v1/tenants/{id}/notifications` | USER or ADMIN |
| `POST` | `/api/v1/tenants/{id}/notifications` | USER or ADMIN |
| `GET` | `/api/v1/tenants/{id}/notifications/{notifId}` | USER or ADMIN |

> **Note:** Swagger UI, `/api-docs`, and `/actuator/health` are public.

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

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user (returns JWT) |
| `POST` | `/api/v1/auth/login` | Login with email + password (returns JWT) |

### Tenants

| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/v1/tenants` | List all active tenants (paginated) | USER |
| `GET` | `/api/v1/tenants/{id}` | Get tenant by ID | USER |
| `POST` | `/api/v1/tenants` | Create a new tenant | ADMIN |
| `DELETE` | `/api/v1/tenants/{id}` | Soft-delete a tenant | ADMIN |

### Channels

| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/v1/tenants/{tenantId}/channels` | List channels for a tenant | USER |
| `GET` | `/api/v1/tenants/{tenantId}/channels/{id}` | Get channel by ID | USER |
| `POST` | `/api/v1/tenants/{tenantId}/channels` | Create a channel | USER |

### Notifications

| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/v1/tenants/{tenantId}/notifications` | List notification history (paginated) | USER |
| `GET` | `/api/v1/tenants/{tenantId}/notifications/{id}` | Get notification by ID | USER |
| `POST` | `/api/v1/tenants/{tenantId}/notifications` | Create and enqueue a notification | USER |

---

## Project Structure

```
src/main/kotlin/com/notificationservice/
├── config/          # Spring configuration (Security, Swagger, Database, AppProperties)
├── controller/      # REST controllers (AuthController, TenantController, ChannelController)
├── domain/
│   ├── model/       # JPA entities (BaseEntity, User, Tenant, Channel, Template)
│   └── enums/       # Role, ChannelType, TemplateStatus
├── dto/
│   ├── request/     # Validated request DTOs (RegisterRequest, LoginRequest, ...)
│   └── response/    # Response DTOs (AuthResponse, ApiResponse envelope, ...)
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── messaging/       # RabbitMQ producers and consumers
├── provider/        # Strategy Pattern implementations for channel delivery
├── repository/      # Spring Data JPA repositories
├── security/        # JwtAuthenticationFilter
├── service/         # Business logic (AuthService, NotificationService, ...)
└── util/            # JwtUtil + entity-to-DTO mapping

src/main/resources/db/migration/
├── V1__init_schema.sql       # Tenants, Channels, Templates tables
├── V2__seed_channels.sql     # Default channel seed data
├── V3__add_users_table.sql   # Users table (BCrypt password hash, role)
└── V4__add_retry_count_to_notifications.sql # Retry tracking column
```

---

## Security Configuration

JWT tokens are signed with HS256. The secret and expiry are configurable via environment variables:

| Variable | Default | Description |
|---|---|---|
| `APP_JWT_SECRET` | *(placeholder — must override)* | HMAC-SHA256 signing secret (min 32 chars) |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24 h) | Token validity in milliseconds |

> **Production warning:** Always set `APP_JWT_SECRET` via a secrets manager or environment variable. Never commit a real secret to source control.

---

## Error Response Format

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

Common HTTP status codes:

| Code | Meaning |
|---|---|
| `401` | Missing or invalid Bearer token |
| `403` | Authenticated but insufficient role |
| `404` | Resource not found |
| `409` | Conflict (duplicate email, name, or slug) |
| `422` | Validation failed |
