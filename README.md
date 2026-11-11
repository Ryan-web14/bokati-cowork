# Bokati Cowork

Backend for coworking space management: bookings, memberships, billing, contracts, payments, and business domiciliation. Built as a modular monolith with Spring Boot 4 and Java 25.

The system covers the operational life of a coworking space end to end. A visitor becomes a lead, a lead becomes a customer, a customer books a resource or takes a subscription, the booking is invoiced, the invoice is paid, and the whole chain is auditable.

## Scope

21 business domains, each self-contained:

| Domain | What it handles |
|---|---|
| `client` | Customers and members |
| `company` | Businesses and coworking spaces |
| `booking` | Resource bookings and holds |
| `ressource` | Resource catalogue, availability, search |
| `subscription` | Passes and recurring subscriptions |
| `billing` | Invoices and PDF generation |
| `payment` | Payment processing, mobile money, cash, transfers |
| `contract` | Contract generation and signing |
| `document` | Document management, KYC, OCR |
| `domiciliation` | Business address services |
| `crm` | Leads and the sales pipeline |
| `inventory` | Product catalogue |
| `event` | Events |
| `visitor` | Visitor and check-in tracking |
| `task` | Internal task management |
| `support` | Support requests |
| `portal` | Client-facing portal |
| `notification` | Email, in-app, and push notifications |
| `reporting` | Operational reports |
| `analytics` | Usage and revenue analytics |
| `verify` | Verification flows |

The `payment` domain is the most developed, with dedicated sub-modules for compliance detection, transaction integrity checks, spending limits, transfers, and payment-side security.

## Architecture

A modular monolith, organised by feature rather than by layer. Three top-level packages:

```
com.sni.bokaticowork
  core/        cross-cutting infrastructure
  features/    business domains
  security/    authentication and authorization
```

### Core

The infrastructure every feature relies on:

- **Outbox** — transactional outbox pattern so domain events and external calls survive a rollback. Processed every 10 seconds in batches of 25.
- **Idempotency** — request deduplication, so a retried call does not double-charge or double-book.
- **Audit** — AOP-based audit logging with its own storage and query API.
- **Generator** — custom sequence engine for human-readable public identifiers, so database primary keys are never exposed.
- **Retry**, **async**, **settings**, **validation**, **rich text**, **templating**, **maintenance**

### Feature structure

Every domain follows the same internal shape, which keeps a codebase of this size navigable:

```
<feature>/
  controller/
  dto/request/, dto/response/
  mapper/interfaces/      MapStruct interfaces
  mapper/decorator/       MapStruct decorators for custom logic
  model/                  JPA entities
  repository/
  service/interfaces/
  service/implementation/
  worker/                 scheduled jobs
```

### Security

JWT authentication with a 15-minute access token and a 7-day refresh token. `JWTFilter` runs before `UsernamePasswordAuthenticationFilter`, and method-level guards are enabled through `@EnableMethodSecurity`. The package also holds rate limiting and admin provisioning.

## Stack

| | |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Database | PostgreSQL 17, Flyway (237 migrations, `ddl-auto=none`) |
| Cache / sessions | Redis |
| Search | Elasticsearch 8.18 |
| Messaging | RabbitMQ, Spring AMQP |
| Object storage | MinIO |
| APIs | REST (SpringDoc OpenAPI), GraphQL (Netflix DGS) |
| Realtime | WebSocket |
| Batch | Spring Batch |
| Mapping | MapStruct |
| Documents | OpenHtmlToPDF, Tess4J (OCR), ZXing (QR codes) |
| Templating | JTE, Thymeleaf, Freemarker |
| Integrations | Microsoft Graph, Azure Identity |

## Running locally

Requires JDK 25 and a Docker daemon.

```bash
git clone https://github.com/Ryan-web14/bokati-cowork.git
cd bokati-cowork

# Start PostgreSQL, Redis, Elasticsearch, RabbitMQ, MinIO, PgAdmin
docker compose up -d

./mvnw spring-boot:run
```

Infrastructure ports: PostgreSQL `5434`, Redis `6379`, Elasticsearch `9200`, PgAdmin `8081`. Credentials come from `.env`, which is not committed. The default Spring profile is `dev`.

API documentation is served at `/swagger-ui.html` once the application is up.

## Build and test

```bash
./mvnw clean install -DskipTests        # build
./mvnw test                             # all tests, Docker required
./mvnw test -DexcludedGroups=integration # unit tests only
./mvnw test -Dtest=CustomerServiceImplTest
```

## Database

Flyway owns the schema. Hibernate's `ddl-auto` is set to `none`, so the schema is never altered by the application. Any change is a new migration file in `src/main/resources/db/migration/`.

## Scheduled workers

| Worker | Frequency |
|---|---|
| Outbox processor | every 10 s, batch of 25 |
| Document processor | hourly |
| Contract processor | hourly |

## Repository layout

```
src/main/java/com/sni/bokaticowork/   application code
src/main/resources/db/migration/      Flyway migrations
src/main/resources/graphql-client/    GraphQL schemas
src/main/jte/                         JTE templates
src/test/                             unit and integration tests
docs/                                 feature-level API specifications
docker/                               service configuration
storage/                              document storage, OCR training data
compose.yaml                          local infrastructure
```

`docs/` holds per-feature API specifications. Consult the relevant document before adding endpoints to a domain.
