# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (skip tests)
./mvnw clean install -DskipTests

# Run application
./mvnw spring-boot:run

# Run all tests (integration tests included, requires a running Docker daemon)
./mvnw test

# Run only unit tests, no Docker needed
./mvnw test -DexcludedGroups=integration

# Run a single test class
./mvnw test -Dtest=CustomerServiceImplTest

# Run a single test method
./mvnw test -Dtest=CustomerServiceImplTest#methodName

# Start required infrastructure (PostgreSQL, Redis, Elasticsearch, PgAdmin)
docker compose up -d
```

## Infrastructure

Services are defined in `compose.yaml`:
- **PostgreSQL 17** on port `5434` — primary database (`cowork_dev_db`)
- **Redis** on port `6379` — session storage and caching
- **Elasticsearch** on port `9200` — search engine
- **PgAdmin** on port `8081`

Dev credentials are in `.env`. The active Spring profile is `dev` by default.

## Architecture Overview

Spring Boot 4 / Java 21 multi-layer monolith. Main package: `com.sni.bokaticowork`.

### Two top-level modules

**`core/`** — cross-cutting concerns used by all features:
- `audit/` — AOP-based audit logging with its own controller, repo, and DTOs
- `generator/` — custom sequence engine for ID generation
- `idempotency/` — idempotent request handling
- `outbox/` — transactional outbox pattern (processed every 10 s, batch 25)
- `baseClasses/` — base entity, base controller, base service
- `configuration/` — Spring bean definitions
- `exception/` — `GlobalExceptionHandler` + custom exception hierarchy
- `communication/` — email via Freemarker templates (Hostinger SMTP)
- `retry/`, `async/`, `settings/`, `validation/`

**`features/`** — business domains (each follows the same internal structure):
- `client/` → `customer/`, `member/`
- `company/` — business/coworking-space entities
- `booking/` — resource bookings and holds
- `inventory/` — product catalog
- `subscription/` — passes and subscriptions
- `billing/` — invoices, payments, PDF generation (OpenHtmlToPDF)
- `contract/` — contract generation and signing
- `document/` — document management and KYC
- `resource/` — resource availability and search (Elasticsearch)
- `payment/` — payment processing
- `notification/`, `analytics/`

**`security/`** — JWT auth, role-based access, user provisioning:
- `filter/JWTFilter` is inserted before `UsernamePasswordAuthenticationFilter`
- `@EnableMethodSecurity` for method-level guards
- Access token: 15 min; Refresh token: 7 days

### Internal feature structure convention

Each feature module follows:
```
<feature>/
  controller/
  dto/
    request/
    response/
  mapper/
    interfaces/      ← MapStruct interfaces
    decorator/       ← MapStruct decorator for custom logic
  model/             ← JPA entities
  repository/
  service/
    interfaces/
    implementation/
```

### Database / migrations

Flyway manages all schema changes (`src/main/resources/db/migration/`, baseline V3, currently at V24). `hibernate.ddl-auto = none`. Never manually alter the schema — always add a new migration file.

### GraphQL

Netflix DGS 11 is used for the GraphQL API. Schemas live in `src/main/resources/graphql-client/`. DGS code generation runs via the `graphqlcodegen-maven-plugin` during the build.

### Template engines

- **JTE** (`src/main/jte/`) — primary server-side templates
- **Thymeleaf** (`src/main/resources/templates/`) — HTML views
- **Freemarker** — email templates

### Scheduled workers

Three background workers (configured in `application.yml`):
- **Document processor** — hourly
- **Contract processor** — hourly
- **Outbox processor** — every 10 s, batch of 25

### API documentation

`docs/` contains detailed feature-level API specs. Consult the relevant doc before adding endpoints to a feature. OpenAPI (SpringDoc) is available at `/swagger-ui.html` when the app is running.