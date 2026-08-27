# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Kavun is an opinionated, production-ready Spring Boot 3.5 starter (Java 21). It combines a Thymeleaf/Bootstrap admin web UI with a versioned REST API (`/api/v1`), JWT auth, dynamic role/permission management, and enterprise concerns (auditing, rate limiting, virus scanning, S3 storage, i18n).

## Build, run, and test commands

```bash
# Run the app (development profile, H2 in-memory DB)
./gradlew bootRun

# Hot reload while developing
./gradlew bootRun --continuous --quiet

# Unit tests only
./gradlew test

# Integration tests only (defaults to the `integration-test` profile, H2 in Postgres mode)
./gradlew integrationTest

# Integration tests against a real Postgres via Testcontainers
SPRING_PROFILES_ACTIVE=integration-test-ci ./gradlew clean build

# Everything (unit + integration)
./gradlew testAll

# Run a single test class / method
./gradlew test --tests "com.kavun.web.rest.v1.UserRestApiTest"
./gradlew test --tests "com.kavun.web.rest.v1.UserRestApiTest.getUserByUsername"

# Full clean build (spotless formatting runs automatically before compile via a JavaCompile dependsOn hook)
./gradlew clean build

# Formatting only
./gradlew spotlessApply

# OWASP dependency vulnerability scan
./gradlew dependencyCheckAnalyze --info

# Refresh dependencies after editing build.gradle
./gradlew build --refresh-dependencies
```

`docker-compose up -d` starts Postgres/supporting services before running the app locally. Active Spring profiles are `development` (default), `docker`, `test`, `production`, plus the two integration-test profiles above.

Local URLs once running: app at `:8080`, H2 console at `:8080/console`, Swagger UI at `:8080/swagger-ui/index.html`, Actuator health at `:8080/actuator/health`.

## Architecture

Layered, package-by-concern structure under `src/main/java/com/kavun/`:

- `backend/persistent/domain/**` — JPA entities, split by bounded area (`user`, `security`, `email`, `upload`, `siem`, `base`). `base/BaseEntity` provides the common id/auditing/soft-delete fields all entities extend.
- `backend/persistent/repository` / `backend/persistent/specification` — Spring Data JPA repositories and `Specification<T>` builders used for dynamic, filterable queries (paired with DataTables server-side processing).
- `backend/service/**` — business logic, one subpackage per domain (`user`, `security`, `mail`, `sms`, `storage`, `i18n`, `scheduled`), each typically with an interface at the package root and an `impl` subpackage. `service/base/BaseService<REQUEST, DTO, ENTITY>` defines the standard CRUD contract (find/create/update/soft-delete/restore/paged findAll) that domain services implement.
- `web/controller` — Thymeleaf MVC controllers serving the server-rendered admin UI.
- `web/rest/v1` — versioned JSON REST API controllers.
- `web/payload/{request,response,pojo}` — API request/response shapes, separate from JPA entities and from `shared/dto`.
- `web/advice/RestResponseEntityExceptionHandler` — global `@ExceptionHandler`; do not catch-and-wrap exceptions locally in controllers, add a handler method here instead (see `docs/API_RESPONSE_STANDARDS.md`).
- `annotation/` + `annotation/impl` — custom annotations backed by AOP aspects, notably `@RequirePermission` (`impl/PermissionAspect`) for endpoint authorization and `@Loggable` (`impl/MethodLogger`) for method-level logging.
- `config/security` — `SecurityConfig`/`ApiWebSecurityConfig` (stateless JWT for the REST API) plus `WebSecurityConfig`, which only permits the handful of public non-API routes (home, login redirect, error pages, actuator/swagger, static assets) and denies everything else — there is no HTML login form or session-authenticated page. Plus `config/security/jwt` (`JwtAuthTokenFilter`, `JwtAuthenticationEntryPoint`).
- `config/properties` — typed `@ConfigurationProperties` classes backing the environment variables documented in `README.md`.
- `enums`, `constant`, `exception` — shared vocabulary; keep magic strings/status codes out of business code.
- `task/` — scheduled jobs (`@Scheduled`).
- `shared/{dto,request,util}` — cross-cutting DTOs, request base types, and utility classes.

### API response contract

All `web/rest/v1` endpoints return the standardized envelope described in `docs/API_RESPONSE_STANDARDS.md` (`timestamp`, `status`, `code`, `message`, `data`, `path`). Controllers can either return plain objects (auto-wrapped) or build an `ApiResponse` explicitly for custom codes/messages — do not resurrect `CustomResponse`, it has been removed. Exception → status/code mapping lives centrally in `RestResponseEntityExceptionHandler`; add new domain exceptions there rather than handling them per-controller.

### Permission system

Endpoint authorization uses the `@RequirePermission` annotation (see `docs/PERMISSION_MANAGEMENT.md`), not raw `@PreAuthorize` role checks, for anything permission-driven (form-login role checks like `hasRole(...)` are still used in a few legacy/admin-only paths). Permissions are `PAGE_CODE:ACTION_CODE` strings. Prefer `@RequirePermission(autoDetect = true)`, which derives `PAGE_CODE` from the URL path (or an incoming `Page-Code` header) and `ACTION_CODE` from the HTTP method (GET→VIEW, POST→CREATE, PUT/PATCH→EDIT, DELETE→DELETE); use `actionOverride` when the HTTP verb doesn't match the real business action (e.g. a `POST /approve/{id}` should require `APPROVE`, not `CREATE`). Fall back to explicit `pageActions = {...}` (OR logic) only for permissions that don't fit the auto-detected page/action.

### Entities and soft delete

Domain services generally implement soft-delete semantics (`delete`/`restore` in `BaseService`) rather than hard deletes; auditing of CRUD operations is handled via Hibernate Envers. When adding a new domain object, follow the existing entity → repository → specification → service (+impl) → payload DTO → REST controller chain (`docs/ENDPOINT.md` walks through this for a new endpoint end-to-end).

## Testing conventions

- Unit tests: JUnit 5 + Mockito, in `src/test/java`, mirroring the main package structure.
- Integration tests live in the separate `src/integrationTest` source set (not under `src/test`), using Testcontainers/Postgres or H2-as-Postgres depending on profile.
- `equals`/`hashCode` on entities are tested with EqualsVerifier; `toString` with jparams `ToStringVerifier`. Follow the patterns in `docs/TESTS.md` rather than hand-rolling these tests.
- S3 interactions in tests are backed by the `s3mock` library, not real AWS — see `docs/TESTS.md` for the test `AmazonS3` bean.
- Coverage (JaCoCo) intentionally excludes `exception`, `enums`, `constant`, `config`, `backend/bootstrap`, mock classes, and generated MapStruct `*DtoMapperImpl` classes — don't chase coverage there.

## Conventions from CONTRIBUTING.md

- Branching follows GitFlow: `feature/*`, `bugfix/*`, `refactor/*` off `develop`; `release/*` off `develop` merging to both; `hotfix/*` off `master` merging to both. PRs target `develop` (not `master`).
- Commit messages follow Conventional Commits, enforced by commitlint/husky: `<type>(optional scope): <description>`, imperative present tense, no leading capital, no trailing period (e.g. `fix: resolve null pointer in token validation`).
- Semantic Versioning (MAJOR.MINOR.PATCH).

## Formatting

Spotless enforces formatting (trailing whitespace trimmed, tabs→spaces, unused imports removed) and runs automatically as a `JavaCompile` dependency — a plain `./gradlew build` will reformat files in place before compiling.
