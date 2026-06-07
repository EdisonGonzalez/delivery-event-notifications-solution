# Agent history (interaction and decisions summary)

This file summarizes the decisions, changes, and steps performed by the agent during the implementation of the project.

## Action summary

- Added Docker / Docker Compose support for local deployment (PostgreSQL + app).
- Created a multi-stage `Dockerfile` for build and runtime with Java 21.
- Added `.dockerignore` to reduce the build context.
- Added migration `V2__mock_data.sql` with sample data in `notification_events`.
- Configured Flyway and executed it on application startup.
- CI: GHCR publishing workflow (`.github/workflows/publish-ghcr.yml`) on PR merge into `main`.
- CI: CI workflow that runs `mvn test` on PRs (`.github/workflows/ci-test.yml`).
- Created `docker-compose.override.yml` for the `dev` profile with alternate ports and logging.
- Updated documentation (`README.md`) to make it clear that PostgreSQL is used (not H2).
- Implemented and validated point 1 (`SKIP LOCKED` concurrency) and point 3 (auditing with `AuditContextConfig`).
- Added integration tests for the REST endpoints that verify client-based authorization.
- **Added JWT authentication** to replace Basic Auth with stateless token-based authentication.
- **Implemented BOLA protection** by including clientId claims in JWT tokens.

## Recent iteration (Flyway 12.8.1 with PostgreSQL 15 and Actuator healthchecks)

- **Fixed Flyway "Unsupported Database" error**: upgraded `flyway-core` to `12.8.1` and added `flyway-database-postgresql:12.8.1` module
- **PostgreSQL 15**: changed Docker image from `postgres:16-alpine` to `postgres:15-alpine` for better Flyway 12.8.1 compatibility
- **Flyway enabled by default**: changed `spring.flyway.enabled: false` to `true` in `application.yml` (default profile)
- **Application healthcheck**: added Docker healthcheck to the `app` service using `/actuator/health` endpoint
  - Interval: 10 seconds
  - Timeout: 5 seconds
  - Retries: 5 attempts
  - Start period: 30 seconds (grace period before health checks begin)
- **Verified migrations**: both `V1__init.sql` (schema) and `V2__mock_data.sql` (sample data) execute successfully on startup

## Notes on important decisions

- PostgreSQL is the only supported database to avoid dialect/locking differences between environments.
- **PostgreSQL 15** is the baseline version for Flyway 12.x compatibility; it is well-supported and LTS.
- **Spring Boot Actuator** `/actuator/health` endpoint is used for Docker healthchecks to provide liveness/readiness probes compatible with Kubernetes.
- Security: JWT authentication with stateless tokens and BOLA protection via clientId claims.
- Retries: `Spring Retry` configured to control backoff in the delivery adapter.

## How to reproduce locally

Start the infrastructure and app (run from the repository root):

```sh
docker compose up --build
```

The output will show both `postgres` and `app` containers reaching healthy state:

```
[+] Running 4/4
 ✔ Network delivery-event-notifications-solution_default       Created                                                                                                         0.1s 
 ✔ Volume delivery-event-notifications-solution_postgres_data  Created                                                                                                         0.0s 
 ✔ Container notifications-postgres                            Healthy                                                                                                         6.5s 
 ✔ Container delivery-event-notifications-app                  Started (Healthy after ~30-40 seconds)
```

Run tests locally:

```sh
mvn test
```

## Relevant code changes

- `docker-compose.yml` (added app healthcheck, updated postgres image to 15-alpine)
- `pom.xml` (added `flyway-database-postgresql:12.8.1`)
- `src/main/resources/application.yml` (enabled Flyway)
- `Dockerfile`, `.dockerignore`
- `src/main/resources/db/migration/V2__mock_data.sql`
- `.github/workflows/publish-ghcr.yml`, `.github/workflows/ci-test.yml`
- `README.md` (updated Docker and Flyway documentation)
- `src/test/java/.../NotificationEventControllerIntegrationTest.java` (integration tests)
- `src/main/java/.../config/JwtUtil.java` (added clientId claim support)
- `src/main/java/.../config/CustomUserDetails.java` (new class with clientId)
- `src/main/java/.../config/CustomUserDetailsService.java` (updated to use CustomUserDetails)
- `src/main/java/.../config/JwtAuthenticationFilter.java` (JWT filter for authentication)
- `src/main/java/.../config/SecurityConfig.java` (JWT security configuration)
- `src/main/java/.../web/AuthController.java` (login endpoint for JWT tokens)
- `src/main/java/.../security/CurrentClientProvider.java` (clientId extraction from JWT)
- `src/main/resources/db/migration/V3__create_users_table.sql` (users table for JWT auth)
- `src/main/resources/db/migration/V4__insert_users.sql` (default users with BCrypt passwords)
- `docs/postman/DeliveryEventNotifications.postman_collection.json` (updated for JWT auth)

## Recent iteration (test stabilization and replay integration)

- The query in `NotificationEventJpaRepository` was adjusted to avoid null-parameter typing issues in PostgreSQL (
  `coalesce` in optional filters).
- Integration tests were stabilized by seeding explicit data in `@BeforeEach` without relying on Flyway in the `test`
  profile.
- Integration cases were added for `POST /notification_events/{id}/replay`:
    - successful replay for a `FAILED` event owned by the authenticated client (`202 Accepted`)
    - replay rejected for a non-`FAILED` event (`409 Conflict`)
    - BOLA/IDOR validation for another client’s event (`404 Not Found`)
- `maven-surefire-plugin` was configured with `forkedProcessExitTimeoutInSeconds=120` to avoid forced-termination
  warnings in Spring Boot + Testcontainers runs.
- Final validation was executed with `mvn test`: full suite green.

## Recent iteration (coverage and async delivery)

- Unit tests were added for `NotificationDeliveryProcessor` covering `IGNORED`, `COMPLETED`, and `recover` failure
  branches.
- An end-to-end HTTP test was added for `WebhookDeliveryAdapter` using the JDK `HttpServer`:
    - successful payload delivery to `/hook`
    - error propagation when the endpoint returns `500`
- The test file was renamed to `WebhookDeliveryAdapterHttpServerTest` to reflect the actual technology used.
- Validation was executed with `mvn test`: `30` tests, `0` failures, `0` errors.
- JaCoCo coverage was updated:
    - Instruction: `86.67%`
    - Branch: `82.50%`
    - Line: `87.66%`
    - Method: `86.23%`

## Recent iteration (JWT Authentication Implementation)

- **JWT Token Generation**: Updated `JwtUtil.generateToken()` to accept both username and clientId parameters
  - Added `clientId` as a custom claim in JWT tokens for BOLA (Broken Object Level Authorization) protection
  - Added `extractClientId()` method to retrieve clientId from JWT tokens
- **Custom UserDetails**: Created `CustomUserDetails` class that extends Spring Security UserDetails
  - Includes `clientId` field alongside username, password, and authorities
  - Enables BOLA protection by storing clientId in the authentication context
- **UserDetailsService Update**: Modified `CustomUserDetailsService` to return `CustomUserDetails` instead of Spring's default User
  - Loads clientId from the `users` table and includes it in the UserDetails
- **Authentication Controller**: Updated `AuthController.login()` to:
  - Extract clientId from CustomUserDetails
  - Pass clientId to JwtUtil.generateToken() for inclusion in JWT claims
- **Security Provider**: Updated `CurrentClientProvider` to:
  - Extract clientId from CustomUserDetails in the security context
  - Validate that the principal is CustomUserDetails before extraction
- **Security Configuration**: Updated `SecurityConfig` to:
  - Use `JwtAuthenticationFilter` for stateless JWT authentication
  - Permit `/auth/login` endpoint for token acquisition
  - Require authentication for all other endpoints except health checks
- **Postman Collection**: Updated to use JWT Bearer token authentication instead of Basic Auth
  - Added Login endpoint to obtain JWT tokens
  - Updated all protected endpoints to use Bearer token authentication
  - Added test script to automatically store JWT token in collection variables

## JWT Security Considerations

**OAuth 2.0 Compliance**: The current implementation is a custom JWT solution, not OAuth 2.0 compliant. For a PoC, this is acceptable but should be documented. OAuth 2.0 would require specific flows (authorization code, client credentials) with token endpoints and scopes.

**Security Best Practices Implemented**:
- BCrypt password hashing (strength 10) for user credentials
- Stateless JWT authentication with configurable expiration (default 24 hours)
- HMAC-SHA256 signing with configurable secret key
- BOLA protection via clientId claims in JWT tokens
- Database-backed user management with enabled/disabled status

**Security Enhancements Needed for Production**:
- Token refresh mechanism
- Token revocation/blacklisting
- Audience and issuer claims
- Stronger secret key management (environment variables, secret vaults)
- Token expiration validation in the filter
- Rate limiting on `/auth/login` endpoint

## Hexagonal Architecture Compliance

JWT components follow hexagonal architecture principles:
- `JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService` in `infrastructure/config` (adapter layer)
- `AuthController` in `infrastructure/web` (HTTP adapter)
- `CustomUserDetails` in `infrastructure/config` (adapter-specific implementation)
- `CurrentClientProvider` in `infrastructure/security` (infrastructure security component)

The authentication logic is properly isolated in the infrastructure layer, with domain logic remaining independent of Spring Security.

## Unit Test Coverage

Added comprehensive unit tests for JWT components:
- `JwtUtilTest`: Tests token generation, username extraction, clientId extraction, and validation
- `CustomUserDetailsTest`: Tests UserDetails implementation with clientId
- `AuthControllerTest`: Tests login endpoint and token generation
- Updated `CurrentClientProviderTest`: Tests clientId extraction from CustomUserDetails

-- END --
