# Multi-Tenancy

Kavun serves many customers (tenants) from one database and one schema. Every row a tenant owns
carries its `tenant_code`, and Hibernate adds `tenant_code = <current tenant>` to every query on
those tables, so application code never filters by tenant and cannot forget to.

## Concepts

- **Tenant** (`tenants` table): a customer, identified by an immutable code such as `acme`.
- **Platform tenant** (`platform`): the operators of the installation. It is created on startup,
  owns the default `admin` user, and its administrators manage all other tenants. It cannot be
  disabled or deleted.
- **Users** belong to exactly one tenant. Usernames and emails are unique across all tenants, so
  login needs nothing but username (or email) and password.
- **Roles and permissions** are per tenant: every tenant has its own `ROLE_ADMIN` and `ROLE_USER`
  and grants permissions to its own roles. Pages and actions (the permission catalog) are shared.

## What is tenant-owned

Entities extending `TenantScopedEntity` are tenant-owned: `User`, `UserRole`, `Role`, `Permission`,
`UserSession`, `UserDevice`, `ApplicationLog`, and every product entity that should be.

Shared, platform-wide data extends `BaseEntity` directly: `Tenant`, `WebPage`, `Action`,
`PageAction`, and short-lived security records (`Otp`, `Captcha`).

```java
@Entity
public class Product extends TenantScopedEntity { ... }   // owned by a tenant
```

## How the current tenant is resolved

`TenantContext.current()` decides, in this order:

1. An explicit `TenantContext.runAs(code, ...)` / `callAs(code, ...)` block.
2. The tenant of the signed-in user (from the `User` row, loaded on every request; there is no
   tenant claim in the token to go stale).
3. `TenantContext.ROOT`: no filter at all, every tenant's data.

Root is what runs when no user is involved: finding the user during login, the JWT filter loading
the user, scheduled jobs (session cleanup, user pruning, permission expiry), startup tasks, and
public endpoints. Writes are stricter than reads: a tenant-owned entity created in the root context
without its tenant set explicitly fails with `IllegalStateException`, so data never lands in the
wrong tenant.

Rules of thumb:

- Inside a request of a signed-in user, do nothing: everything is scoped already.
- Background work that must act for one tenant: `TenantContext.runAs(code, ...)`, and open a new
  transaction inside the block (a database session keeps the tenant it was opened with).
- A `@PublicEndpoint` runs in the root context. Do not list tenant data from one unless you pick
  the tenant explicitly.
- Spring caches (`@Cacheable`) are not tenant-aware. Do not cache tenant-owned data with a key
  that does not include the tenant.

## Tenant management

Platform administrators manage tenants through `/api/v1/tenant` (page `TENANTS`, platform-only):

```http
POST /api/v1/tenant
{
  "code": "acme",
  "name": "Acme Jewellery",
  "adminUsername": "acme-admin",
  "adminEmail": "admin@acme.example",
  "adminPassword": "a-strong-password"
}
```

Creating a tenant provisions it, all as that tenant: its `ROLE_ADMIN` and `ROLE_USER`, the catalog
permissions of its `ROLE_ADMIN`, and the administrator from the request. If any step fails nothing
is kept. The tenant administrator then manages users, roles and permissions of that tenant only.

- Codes are lowercase letters, digits and single hyphens, at most 50 characters, and never change.
- `PUT /api/v1/tenant/{id}` with `"enabled": false` (or deleting the tenant) locks its users out
  on their next request, including tokens they already hold.

## Platform-only pages

Some pages act on every tenant and are reserved for the platform tenant: `TENANTS`, and the shared
catalog pages `PAGES`, `ACTIONS` and `PAGE_ACTIONS`. Mark such pages with `.platformOnly()` in the
permission catalog. They are:

- granted only to the platform's `ROLE_ADMIN` by the catalog synchronizer,
- refused to users of other tenants even if granted or with the admin bypass on,
- neither shown in the permission matrix of other tenants nor grantable there (matrix or Permission API).

## Upgrading an existing database

New databases get the tenant columns and the per-tenant role name constraint from the Liquibase
baseline. A database created earlier by Hibernate's `ddl-auto` is accepted as it is (see
`docs/DATABASE_MIGRATIONS.md`), with two things to check:

- It must have the `tenant_code` columns, which `ddl-auto=update` added when the application first
  ran with tenancy. Without them the startup fails, naming the missing column; recreate such a
  development database.
- Role names used to be unique across the table and are now unique per tenant. Drop the old
  constraint, otherwise provisioning a tenant fails on its `ROLE_ADMIN`:

```sql
-- PostgreSQL: find the unique constraints on role.name ...
SELECT con.conname, pg_get_constraintdef(con.oid)
FROM pg_constraint con
WHERE con.conrelid = 'role'::regclass AND con.contype = 'u';

-- ... and drop the ones defined only on (name), keeping (tenant_code, name)
ALTER TABLE role DROP CONSTRAINT <constraint_name>;
```
