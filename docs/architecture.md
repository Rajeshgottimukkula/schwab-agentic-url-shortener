# Architecture

## Scope

The application is a Java 21 and Spring Boot modular monolith. Its approved first API scope is:

- `POST /api/v1/urls` to validate an HTTP or HTTPS destination, create a secure random alphanumeric code, persist a mapping, and return `201 Created`.
- `GET /{code}` to resolve a mapping, atomically increment its aggregate click count, and redirect with `302 Found`. Unknown and expired codes return `404 Not Found`.
- `GET /api/v1/urls/{code}` to return mapping information, including the click count, without exposing JPA entities or persistence details.

The application implements the approved URL-shortener API scope described above. The implementation was built incrementally across the engineering tasks, with architecture and persistence established first and the API/business operations added and validated in subsequent tasks.

## Modules and responsibilities

Packages are organized by responsibility within one deployable application:

- `api`: controllers and request/response DTOs. DTOs are distinct from JPA entities.
- `service`: application use cases, code generation coordination, expiry rules, and transactional boundaries.
- `domain`: URL mapping model and domain rules.
- `persistence`: Spring Data repositories and persistence-specific queries.
- `config`: externalized settings and Spring infrastructure configuration.
- `error`: centralized exception mapping to consistent `ProblemDetail` responses.

The intended create flow is API validation → service → repository → response DTO. Redirect resolution should perform lookup, expiry handling, and an atomic click-count increment within the service/persistence boundary. Destination URLs must never be fetched by this service.

## Persistence model

MySQL is the source of truth. Flyway migrations under `src/main/resources/db/migration` define the schema. Hibernate uses `ddl-auto: validate`; schema creation and updates are not delegated to Hibernate.

The initial `short_urls` table stores:

- `id`: internal numeric primary key.
- `short_code`: unique public code, with a database-level unique constraint.
- `original_url`: validated destination.
- `created_at`: creation time in UTC.
- `expires_at`: nullable; null means no expiration.
- `click_count`: non-negative aggregate, initialized to zero.

The aggregate click counter is intentionally simple. Updating one row per redirect can create write contention for a highly popular code. This design does not collect IP addresses, user agents, referrers, or other personal tracking data. More elaborate event analytics are outside the approved scope.

## Code generation and URL validation

The planned code generator uses a cryptographically secure random source, an alphanumeric alphabet, a fixed configurable length, and bounded retries when a generated code conflicts. The database unique constraint remains authoritative under concurrent requests.

The planned URL validator requires a nonblank absolute URL, an HTTP or HTTPS scheme, and a host. Malformed input is rejected. No preview, metadata lookup, or other destination fetch is part of the design; this avoids introducing an SSRF-prone request path.

## API and error behavior

Create returns `201 Created`; valid redirects return `302 Found` with `Location` set to the stored destination. Unknown and expired codes return `404 Not Found`. API failures use Spring `ProblemDetail` with stable client-safe messages. SQL details and stack traces must not be returned to clients.

## Configuration and operations

Database URL, username, and password are supplied through `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. No credentials belong in source control. Short-code length is configurable through `SHORT_URL_CODE_LENGTH`. Actuator exposes health only over HTTP, with readiness/liveness health probes enabled.

## Testing approach

- Unit tests cover URL validation, code generation/collision handling, and expiry rules.
- MVC/API tests cover request validation, status codes, response DTOs, redirects, and error shapes.
- MySQL integration tests cover migration application, entity/repository mappings, uniqueness, atomic click updates, and transaction behavior.
- Failure and boundary cases include malformed URLs, maximum lengths, collisions, missing/expired codes, and database failures.
