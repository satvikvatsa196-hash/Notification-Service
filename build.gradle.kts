import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.notificationservice"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // ── Spring Boot Core ──────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ── Kotlin ────────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ── Messaging ─────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // ── Database ──────────────────────────────────────────────────────────────
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // ── API Documentation ─────────────────────────────────────────────────────
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // ── Logging ───────────────────────────────────────────────────────────────
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // ── Redis ─────────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // ── Monitoring ────────────────────────────────────────────────────────────
    implementation("io.micrometer:micrometer-registry-prometheus")


    // ── Security ──────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ── JWT (JJWT 0.12.x) ────────────────────────────────────────────────────
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // ── Test ──────────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("com.redis.testcontainers:testcontainers-redis:2.2.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Ensure all-open plugin applies to JPA entities
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
