# AI-Assisted URL Shortener

## 1. Objective

This Java 21 URL shortener is an engineer-led, AI-assisted software engineering prototype. It demonstrates requirement understanding, task decomposition, incremental implementation, validation, and reviewable engineering output. AI supports the work; engineering decisions and sign-off remain with the engineer.

## 2. Engineering Approach

The workflow is:

**Requirement → task decomposition → AI-assisted implementation → engineer review, edits, or rejection → automated validation → final sign-off**

The engineer owns correctness, maintainability, security, and production-readiness decisions. Work is split into scoped tasks, with explicit constraints, acceptance criteria, validation, trade-offs, and review points recorded in `docs/ai-engineering-log.md`.

## 3. Architecture

The application is a Spring Boot modular monolith, organized by responsibility and deployed as one application:

| Package | Responsibility |
| --- | --- |
| `api` | Controllers and request/response DTOs |
| `service` | Creation, lookup, and redirect use cases and business rules |
| `domain` | URL validation, short-code generation, and URL mapping model |
| `persistence` | Spring Data JPA repository and atomic click-count update |
| `config` | Spring infrastructure configuration, including UTC `Clock` |
| `error` | Centralized sanitized `ProblemDetail` responses and diagnostic logging |

MySQL is the source of truth. Flyway owns versioned schema migrations, and Hibernate uses `ddl-auto: validate`; Hibernate does not create or update the production schema. API DTOs and service result records keep JPA entities and persistence details out of controller responses.

## 4. Implemented APIs

| Method and path | Behavior |
| --- | --- |
| `POST /api/v1/urls` | Validates an HTTP/HTTPS destination, generates a cryptographically secure random alphanumeric short code, persists the mapping, and returns `201 Created`. An optional expiration timestamp is accepted. |
| `GET /{code}` | Resolves a mapping, rejects unknown or expired codes with `404`, atomically increments `click_count` for an active mapping, and returns `302 Found` with `Location` set to the stored destination. |
| `GET /api/v1/urls/{code}` | Returns mapping metadata including code, original URL, expiry, click count, and a request-derived short URL. It does not increment the click count. Unknown codes return `404`. |

Errors use Spring `ProblemDetail` with client-safe messages. Internal exception messages, SQL details, and stack traces are not returned in API responses.

## 5. Data Model

Flyway migration `V1__create_short_url_table.sql` creates `short_urls` with:

| Field | Purpose |
| --- | --- |
| `id` | Internal numeric primary key |
| `short_code` | Public code with a database-level unique constraint |
| `original_url` | Validated HTTP/HTTPS destination |
| `created_at` | Mapping creation instant |
| `expires_at` | Optional expiration instant; null means no expiration |
| `click_count` | Non-negative aggregate click count initialized to zero |

## 6. AI-Assisted Engineering Workflow

The engineering log records each task’s intent, constraints, AI-generated output, accepted changes, rejected or avoided changes, validation, trade-offs, and engineer review/sign-off points. It covers the progression from architecture and persistence scaffolding through URL validation, code generation, create, redirect, lookup, and error-logging work.

As documented there, AI assistance supported implementation, debugging and refinement, test development, documentation, and review preparation. The engineer reviewed the scope and output, selected what to accept, and remained responsible for final decisions. The workflow is not autonomous.

## 7. Three Engineering Scenarios

### 7.1 Greenfield Scenario

The greenfield-style work began by understanding the URL-shortener requirements and separating API, service, domain, persistence, configuration, and error responsibilities. It established MySQL, JPA, Flyway, and schema validation first, then added capabilities incrementally: URL validation and code generation, creation, redirect, and lookup. Migration and schema behavior were validated against MySQL as the implementation evolved.

### 7.2 Brownfield Scenario

Later tasks evolved an existing codebase, following its package boundaries and preserving existing behavior:

- Redirect handling reused the repository and centralized error architecture, adding an atomic update for concurrent click counting.
- The lookup API reused the existing repository lookup and not-found handling.
- Security hardening centralized diagnostic logging and safe error details without adding unrelated infrastructure.

Each change kept a narrow surface and retained existing creation, validation, persistence, and migration behavior.

### 7.3 Ambiguous Scenario

The engineering log documents decisions for these ambiguous areas:

- **Expiration boundary:** A mapping expires only when `expiresAt` is before the service’s captured current instant. Equality remains active. This makes the boundary explicit and testable.
- **Lookup of expired mappings:** Lookup returns stored metadata, including expiry, even if the mapping is expired. Expiration blocks redirects; the mapping is not deleted, and lookup was not specified to hide it.
- **Concurrent click counts:** A database-side `click_count = click_count + 1` update avoids lost increments that a read-modify-save sequence could cause. A load or concurrency stress test was not performed.
- **Error diagnostics:** Server logs include a fixed event label, request URI path, and exception class. Exception messages and stack traces are omitted to avoid logging sensitive database/request details; clients receive sanitized messages.
- **Destination validation:** Validation is syntactic only and permits HTTP/HTTPS URLs with a host. The application does not fetch or probe destinations, avoiding an SSRF-prone preview path.

## 8. Key Engineering Decisions

- Validate destination syntax only; allow HTTP and HTTPS with a host.
- Never make outbound destination requests.
- Generate alphanumeric codes with `SecureRandom` and configurable length.
- Bound code-collision retries; the database unique constraint remains authoritative.
- Increment `click_count` atomically in MySQL.
- Inject a UTC `Clock` to make expiration behavior deterministic in tests.
- Keep API DTOs separate from JPA entities.
- Return sanitized `ProblemDetail` errors and generic 500 details.
- Log only safe operational context for persistence and unexpected failures.
- Expose only Actuator health endpoints.
- Supply database credentials through environment variables, not source control.

## 9. Validation and Quality Gates

The final full Maven test run completed with **46 tests, 0 failures, 0 errors, and 0 skipped**. Flyway successfully validated the existing migration, and Hibernate initialized with schema validation enabled. `git diff --check` passed, as did a credential/secret scan of source and configuration.

### Manual API smoke test

The running application was also manually validated against local MySQL:

1. `POST /api/v1/urls` returned `201 Created` and generated short code `zQh4A5VB`.
2. `GET /api/v1/urls/zQh4A5VB` returned the stored mapping with `clickCount = 0`.
3. `GET /zQh4A5VB` returned `302 Found` with `Location: https://example.com`.
4. A subsequent lookup showed `clickCount = 2`, confirming successful redirect analytics updates.
5. `GET /actuator/health` returned `200` with health status `UP`.

The redirect was invoked twice during the manual smoke test, hence the final count of 2.

No load test, security penetration test, benchmark, or concurrency stress test is claimed. The atomic-update tests verify repeated increments against MySQL, not concurrent throughput or behavior under production load.

## 10. Setup

The project uses Java 21, Spring Boot 4.1.1, the Maven wrapper, and MySQL 8.x. Configure the database through environment variables; do not put credentials in the repository.

Required variables:

- `DB_URL` — JDBC URL for the application database (for example, `jdbc:mysql://localhost:3306/url_shortener`)
- `DB_USERNAME` — database username
- `DB_PASSWORD` — database password

Optional variable:

- `SHORT_URL_CODE_LENGTH` — short-code length; defaults to `8` and must be within the supported 1–32 character range.

## 11. Running the Application

Set environment variables before running. In Windows PowerShell, use placeholders for local values:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/url_shortener'
$env:DB_USERNAME = '<database-username>'
$env:DB_PASSWORD = '<database-password>'
$env:SHORT_URL_CODE_LENGTH = '8' # optional
```

Run tests or start the app using the wrapper:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

On Unix-like shells:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

## 12. Testing

The test suite has three layers:

- **Unit tests:** URL validation, short-code generation, collision handling, redirect and lookup rules, and expiration boundaries.
- **MVC/API tests:** create and lookup responses, request-derived URLs, redirects, ProblemDetail mappings, and sanitized persistence/unexpected errors.
- **MySQL integration tests:** Spring context and schema validation, Flyway migration validation, entity/repository persistence, atomic click updates, and lookup behavior that leaves click count unchanged.

Tests also cover unknown and expired codes, API validation failures, and the configured health-only Actuator exposure. Integration tests require a reachable MySQL test database.

## 13. Security and Risk Controls

- Database credentials are externalized and are not included in source.
- URL validation is syntax-only; the service makes no destination requests.
- Request bodies and headers are not logged.
- Server-side failure logs omit exception messages and stack traces.
- Client responses do not expose exception details, SQL errors, or credentials.
- Actuator web exposure remains limited to health.
- High-impact behavior and security decisions remain subject to engineer review.

These controls reduce specific exposure risks but do not constitute a security audit or guarantee production security.

## 14. Limitations and Trade-offs

- Each successful redirect writes the click aggregate. A popular code can become a database write hotspot.
- Logging omits exception messages and stack traces by design; deeper diagnostics require secure server/database diagnostic channels.
- Short URLs are derived from the incoming request. Real deployments behind proxies may need correct forwarded-header handling.
- Analytics are limited to `click_count`; there is no distributed analytics or event pipeline.
- Authentication, rate limiting, tracing, and a metrics system were outside the approved assessment scope.

The implementation demonstrates the approved assessment behavior; it does not claim production-scale capacity or operational coverage for those omitted capabilities.

## 15. Final Engineering Summary

This prototype demonstrates requirement decomposition, incremental AI-assisted implementation, brownfield reasoning, explicit engineering decisions, automated validation, security and risk controls, and human review/sign-off. AI contributed to engineering tasks under engineer direction; correctness and production-readiness remain engineering responsibilities.
