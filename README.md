# Eventing Modules

This repository holds the modular pieces for the Innologic eventing stack. Each module can be built independently or together via the parent POM.

## Modules

- **eventing-core** – shared DTOs (e.g. `EventEnvelope`) and utility classes (`TopicNaming`). Provides Jackson-friendly types for serializing / deserializing event payloads.
- **eventing-outbox-jpa** – JPA entities (`OutboxEventEntity`, `ProcessedEventEntity`) plus Flyway migrations and repositories for the MariaDB-based outbox implementation. Also bundles an `OutboxPublisher`.
- **eventing-starter** – Spring Boot auto-configuration that wires together the dispatcher, idempotency, and an `EventBus`. Includes tests and an auto-configuration declarative drop-in (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) so services simply add the starter dependency and configuration properties.

## Building

Use the Maven wrapper at the repository root to build individual modules or the entire multi-module project:

```bash
mvn -pl :eventing-starter -am test
```

## Configuration

The starter exposes `EventingDispatcherProperties` (`eventing.dispatcher.*`) for tuning batching, retries, and backoff, plus `eventing.service-name` for topic naming. Enabled by default; disable with `eventing.dispatcher.enabled=false`.

