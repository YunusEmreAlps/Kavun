# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Kavun is an opinionated, production-ready Spring Boot 3.5 starter (Java 21). It combines a Thymeleaf/Bootstrap admin web UI with a versioned REST API (`/api/v1`), JWT auth, dynamic role/permission management, and enterprise concerns (auditing, rate limiting, virus scanning, S3 storage, i18n).

## Build, run, and test commands

```bash
# Run the app (development profile, PostgreSQL on localhost:5432/kavun - see docker-compose)
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

`docker-compose up -d` starts Postgres/supporting services before running the app locally. Active Spring profiles are `development` (default), `docker`, `test`, `production`, plus the two integration-test profiles above. `application-local.properties` is not a profile of its own - it is an overlay of machine-specific overrides (devtools, personal datasource) and must be activated last, e.g. `SPRING_PROFILES_ACTIVE=development,local`; nothing in the code or in `logback-spring.xml` is bound to the `local` profile name.

Local URLs once running: app at `:8080`, H2 console at `:8080/console`, Swagger UI at `:8080/swagger-ui/index.html`, Actuator health at `:8080/actuator/health`.

## Architecture

Layered, package-by-concern structure under `src/main/java/com/kavun/`:

- `backend/persistent/domain/**` — JPA entities, split by bounded area (`user`, `security`, `email`, `upload`, `siem`, `base`). `base/BaseEntity` provides the common id/auditing/soft-delete fields all entities extend.
- `backend/persistent/repository` / `backend/persistent/specification` — Spring Data JPA repositories, and `FilterSpecification`, the single generic builder that turns list filters (`name=..`, `createdAtFrom=..`, `parentId=..`) into queries over the fields a service allows. Do not write per-entity `Specification` classes.
- `backend/service/**` — business logic, one subpackage per domain (`user`, `security`, `mail`, `sms`, `storage`, `i18n`, `scheduled`), each typically with an interface at the package root and an `impl` subpackage. `service/base/BaseService<REQUEST, DTO>` defines the standard CRUD contract (find/create/update/soft-delete/restore, filtered findAll/count), implemented once by `AbstractService`; a domain service extends it and overrides `filterableFields()` (an explicit allow-list: never include sensitive fields). Soft deleted records are excluded from lists unless `deleted=true`.
- `web/controller` — Thymeleaf MVC controllers serving the server-rendered admin UI.
- `web/rest/v1` — versioned JSON REST API controllers.
- `web/payload/{request,response,pojo}` — API request/response shapes, separate from JPA entities and from `shared/dto`.
- `web/advice/RestResponseEntityExceptionHandler` — global `@ExceptionHandler`; do not catch-and-wrap exceptions locally in controllers, add a handler method here instead (see `docs/API_RESPONSE_STANDARDS.md`).
- `annotation/` + `annotation/impl` — custom annotations: the endpoint access annotations `@PublicEndpoint`/`@AuthenticatedEndpoint`/`@RequirePermission` (enforced by `config/security/authorization`), and `@Loggable` (AOP aspect `impl/MethodLogger`) for method-level logging.
- `config/security` — `SecurityConfig`/`ApiWebSecurityConfig` (stateless JWT for the REST API) plus `WebSecurityConfig`, which only permits the handful of public non-API routes (home, login redirect, error pages, actuator/swagger, static assets) and denies everything else — there is no HTML login form or session-authenticated page. Plus `config/security/jwt` (`JwtAuthTokenFilter`, `JwtAuthenticationEntryPoint`) and `config/security/authorization` (endpoint policy resolution, enforcement and startup validation).
- `config/properties` — typed `@ConfigurationProperties` classes backing the environment variables documented in `README.md`.
- `enums`, `constant`, `exception` — shared vocabulary; keep magic strings/status codes out of business code.
- `task/` — scheduled jobs (`@Scheduled`).
- `shared/{dto,request,util}` — cross-cutting DTOs, request base types, and utility classes.

### API response contract

All `web/rest/v1` endpoints return the standardized envelope described in `docs/API_RESPONSE_STANDARDS.md` (`timestamp`, `status`, `code`, `message`, `data`, `path`). Controllers can either return plain objects (auto-wrapped) or build an `ApiResponse` explicitly for custom codes/messages — do not resurrect `CustomResponse`, it has been removed. Exception → status/code mapping lives centrally in `RestResponseEntityExceptionHandler`; add new domain exceptions there rather than handling them per-controller.

### Permission system

See `docs/PERMISSION_MANAGEMENT.md`. Every `/api` endpoint must carry exactly one of `@PublicEndpoint`, `@AuthenticatedEndpoint` or `@RequirePermission` (on the method or its controller); `EndpointPolicyValidator` refuses to start the application otherwise, and `EndpointAuthorizationInterceptor` (Spring MVC interceptor on `/api/**`) enforces them before the request body is read; the security filter chain itself only authenticates. Never authorize from request headers or paths. Permissions are `PAGE_CODE:ACTION_CODE`: put `@RequirePermission(page = Pages.X)` on the controller, the action follows the HTTP method (GET→VIEW, POST→CREATE, PUT/PATCH→EDIT, DELETE→DELETE), and set `action = ...` where the verb differs (read-only `POST /search` → VIEW, restore → DELETE, role assignment → ASSIGN). Pages and actions must be declared in a `PermissionCatalogContributor` (platform pages: `CorePermissionCatalog`, codes in `PermissionConstants`); `PermissionCatalogSynchronizer` writes them to the DB on startup and grants new ones to `ROLE_ADMIN`. Programmatic checks and the anti-escalation rules (ROLE_ADMIN assignment, admin accounts, built-in roles) go through `AccessControlService`.

### Multi-tenancy

See `docs/MULTI_TENANCY.md`. One schema, a `tenant_code` column on every tenant-owned table (entities extending `TenantScopedEntity`), and Hibernate `@TenantId` filtering every query by `TenantContext.current()`: an explicit `TenantContext.runAs` block, else the signed-in user's tenant, else `ROOT` (unfiltered; login, scheduled jobs, startup, public endpoints). Never filter by tenant by hand; never create tenant-owned rows in the root context without setting the tenant (it throws); never `@Cacheable` tenant-owned data with a tenant-less key. Usernames/emails are unique across tenants; roles and permissions are per tenant (each tenant has its own `ROLE_ADMIN`). The `platform` tenant's admins manage tenants (`/api/v1/tenant`, provisioning roles + first admin); pages acting on all tenants are `.platformOnly()` in the catalog. Integration tests run as the platform tenant (`PlatformTenantExtension`); MockMvc requests resolve their tenant from the signed-in user.

### Entities and soft delete

Entity identity is defined once in `BaseEntity`: `equals`/`hashCode` are `final`, compare the persistent class (Hibernate-proxy aware) and `publicId`, which is assigned on construction. Do not add `equals`/`hashCode` or Lombok `@EqualsAndHashCode` to entities (it will not compile); compare business keys explicitly where needed (see `User.isSameRole`).

Domain services generally implement soft-delete semantics (`delete`/`restore` in `BaseService`) rather than hard deletes; auditing of CRUD operations is handled via Hibernate Envers. When adding a new resource, follow `docs/ENDPOINT.md`: entity → Liquibase changeset → repository → request/DTO/mapper → service extending `AbstractService` → controller extending `web/rest/AbstractCrudRestApi` (standard CRUD endpoints) with `@RequirePermission(page = ...)` → page in a permission catalog contributor.

### Database schema

See `docs/DATABASE_MIGRATIONS.md`. Liquibase owns the schema in every profile (`src/main/resources/db/changelog`, master file includes `changes/NNN-*.xml` in order) and Hibernate only validates it (`ddl-auto=validate`), so every entity change needs a new changeset in the same commit — never edit an applied changeset. Use `${type.text}` for unbounded text columns (H2 turns `TEXT` into a CLOB the validation rejects), plain `VARCHAR` without CHECK constraints for enums, and `tenant_code VARCHAR(50) NOT NULL` without a default on tenant-owned tables. Reference data (roles, permission catalog, platform tenant, default admin) is seeded by the application, not by changesets.

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
