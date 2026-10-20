# Database Migrations

Liquibase owns the database schema. It runs on startup in every profile and applies the changesets
under `src/main/resources/db/changelog`; Hibernate only validates (`ddl-auto=validate`) that the
entities match the tables and refuses to start otherwise. Nothing creates or alters tables behind
your back, so the schema a developer runs is the schema production gets.

```text
db/changelog/
├── db.changelog-master.xml   # includes the files below, in order
└── changes/
    └── 001-baseline.xml      # the schema when Liquibase took over, one changeset per table
```

## Changing the schema

Every entity change needs a changeset, in the same commit:

1. Change the entity.
2. Add a file `changes/NNN-short-description.xml` (next number) with one changeset per logical
   change, and include it at the end of `db.changelog-master.xml`.
3. Run `./gradlew integrationTest`: the changesets run on H2 and Hibernate validates the result.
   Starting the application against PostgreSQL validates it there too.

```xml
<changeSet id="002-product-sku" author="kavun">
  <addColumn tableName="products">
    <column name="sku" type="VARCHAR(50)"/>
  </addColumn>
</changeSet>
```

A new resource (see `docs/ENDPOINT.md`) gets a `createTable` changeset with the columns of
`BaseEntity` (copy them from a table in `001-baseline.xml`) plus `tenant_code VARCHAR(50) NOT NULL`
if it extends `TenantScopedEntity`.

Never edit a changeset that has been applied anywhere: Liquibase stores a checksum per changeset and
refuses to start when one changes. Fix mistakes with a new changeset. On your own development
database you can instead drop the database and let Liquibase recreate it.

To see the DDL Hibernate expects for the current entities (handy when writing a `createTable`),
export it from a test run:

```bash
SPRING_APPLICATION_JSON='{"spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action":"create","spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target":"build/schema.sql"}' \
  ./gradlew integrationTest --tests "com.kavun.backend.service.i18n.I18NServiceIntegrationTest"
```

The DDL (PostgreSQL dialect) ends up in `build/schema.sql`.

## Conventions

- **Names**: `pk_<table>`, `uk_<table>_<columns>`, `fk_<table>_<target>`; index names come from the
  entity's `@Index`.
- **Text**: use `type="${type.text}"` for unbounded text (`@Column(columnDefinition = "text")`). It
  is `TEXT` on PostgreSQL; H2 would turn `TEXT` into a CLOB, which the validation rejects for
  `String` fields.
- **Enums**: plain `VARCHAR` without a `CHECK` constraint, so adding an enum constant needs no
  migration.
- **Tenant column**: `tenant_code` has no default; the application always sets it.
- **Reference data** (roles, the permission catalog, the platform tenant, the default admin) is
  created by the application on startup (`DatabaseSeeder`, `PermissionCatalogSynchronizer`), not by
  changesets. Changesets only move data when a schema change requires it.

## Databases created before the baseline

Databases created by Hibernate's `ddl-auto` already have the tables. Each baseline changeset checks
whether its table exists and, if so, is only marked as ran. Hibernate's validation then checks the
existing columns against the entities, so a missing column fails the startup with its name.

Such databases keep the constraint names PostgreSQL or Hibernate gave them (`users_email_key`, or
`UK`/`FK` followed by a hash) instead of the baseline's names. A later changeset that drops or
renames a baseline constraint by name fails on them; recreate development databases when
convenient.

## Production

- The database and the schema (`DATABASE_SCHEMA`, default `kavun`) must exist; Liquibase creates
  the tables, including its own `databasechangelog` and `databasechangeloglock`, inside it.
- The application's database user needs the right to create and alter tables in that schema.
- Several instances starting at once are safe: Liquibase takes a lock and the others wait.

## Tests

The default integration test profile runs every changeset on H2 in PostgreSQL mode and validates
the schema, so a missing changeset fails the build. The `integration-test-ci` profile does the same
on a real PostgreSQL (Testcontainers):

```bash
SPRING_PROFILES_ACTIVE=integration-test-ci ./gradlew integrationTest
```
