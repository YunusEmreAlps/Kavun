# Liquidbase

## Introduction

Liquibase is an open-source database migration tool that helps you manage, track, and automate schema changes across multiple database systems (PostgreSQL, MySQL, Oracle, SQL Server, etc.).

It allows developers to define database changes in a structured way, making it easier to apply, roll back, and document changes. This is particularly useful in environments where multiple developers are working on the same codebase, as it ensures that everyone is on the same page regarding the database schema.

## Key Features

- **Change Management:** Track, version, and apply schema changes.
- **Refactoring:** Rename tables/columns, add/drop columns, and more.
- **Rollback:** Revert changes in case of errors.
- **Documentation:** Maintain a history of schema changes.
- **Automation:** Integrate with CI/CD for automated migrations.

## Getting Started

1. **Configure your database in `application.properties`:**

    ```properties
    url=jdbc:postgresql://localhost:5432/mydatabase
    username=root
    password=password
    driver=org.postgresql.Driver
    ```

2. **Organize your changelogs:**
    - Place scripts under `src/main/resources/db/changelog`.
    - Scripts are executed in order, based on their inclusion in the main changelog file.

> **More info:** See the [Liquibase documentation](https://www.liquibase.org/documentation/).

---

## Common Error: Validation Failed

You may see an error like:
```text
liquibase.exception.ValidationFailedException: Validation Failed:
     2 changesets check sum
          db/changelog/db-scripts/003-table-changelog.xml::users_0::Yunus Emre Alpu was: 9:5f8b2fda29b32fbf79ccec4d432ca7ae but is now: 9:4c55cc2069d2cf9960d44a2dcb232085
          db/changelog/db-scripts/003-table-changelog.xml::users_audit_log_0::Yunus Emre Alpu was: 9:8436345a1dc47bc1f3907a8eb5c368ae but is now: 9:f7790a9b17ea70d680701c4e91f32db0
```
This happens if you **edit an existing changeset** after it has been applied. Liquibase uses checksums to detect such changes and prevent accidental or malicious modifications.

---

## How to Fix Checksum Errors

### Development Environment (Safe to Reset)

1. **Clear Liquibase checksums:**

    ```bash
    liquibase clearCheckSums
    ```

    or, for Gradle users:

    ```bash
    ./gradlew liquibaseClearCheckSums
    ```

2. **Restart your application.**
    - Liquibase will recalculate checksums and continue.

---

### Production Environment (Do NOT Edit Existing Changesets!)

- **Never edit existing changesets in production.**
- Always add a new `<changeSet>` for any schema change.
- If you must fix a checksum mismatch in production, update the `MD5SUM` column in the `DATABASECHANGELOG` table **only if you are certain the changeset is correct**:

    ```sql
    UPDATE DATABASECHANGELOG
    SET MD5SUM = '9:4c55cc2069d2cf9960d44a2dcb232085'
    WHERE ID = 'users_0' AND AUTHOR = 'Yunus Emre Alpu' AND FILENAME = 'db/changelog/db-scripts/003-table-changelog.xml';

    UPDATE DATABASECHANGELOG
    SET MD5SUM = '9:f7790a9b17ea70d680701c4e91f32db0'
    WHERE ID = 'users_audit_log_0' AND AUTHOR = 'Yunus Emre Alpu' AND FILENAME = 'db/changelog/db-scripts/003-table-changelog.xml';
    ```

    > **Warning:** Editing checksums in production can lead to data inconsistencies. Only do this if you fully understand the implications.

---

## Summary

- **Development:** Clear checksums and restart if you change a changeset.
- **Production:** Never edit applied changesets; always add new ones.
- **Best Practice:** Drop and recreate the database in development if needed.

---

**Tip:**  
If you are still in development, you can drop your database and let Liquibase recreate everything from scratch for a clean state.
