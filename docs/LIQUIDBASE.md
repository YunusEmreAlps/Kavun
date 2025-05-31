# Liquidbase

## Introduction

Liquidbase is a database migration tool that helps you manage your database schema. It is an open-source project that supports a variety of databases including MySQL, PostgreSQL, Oracle, and SQL Server.

## Features

- **Database Change Management**: Liquidbase helps you manage your database schema by providing a way to track, version, and apply changes to your database.
- **Database Refactoring**: Liquidbase supports a variety of refactoring operations such as renaming tables, columns, and indexes, adding new columns, and dropping existing columns.
- **Database Rollback**: Liquidbase allows you to rollback changes to your database schema in case of errors or issues.
- **Database Documentation**: Liquidbase provides a way to document your database schema changes and track the history of your database schema.
- **Database Automation**: Liquidbase can be integrated with your build and deployment process to automate the application of database changes.

## Getting Started

To get started with Liquidbase, you need to create a `application.properties` file that contains the configuration settings for your database. You can then create a `changelog.xml` file that contains the changes you want to apply to your database schema.

Here is an example `application.properties` file:

```properties
url=jdbc:postgresql://localhost:5432/mydatabase
username=root
password=password
driver=com.postgresql.Driver
```

You can access the liquidbase scripts under the `src/main/resources/db/changelog` directory. The scripts are organized by version and are executed in order. The scripts are executed in the order of the version number.

> **Note**: You can find more information about Liquidbase in the official documentation [Documentation](https://www.liquidbase.org/documentation/).

## Error: Validation Failed

```text
liquibase.exception.ValidationFailedException: Validation Failed:
     2 changesets check sum
          db/changelog/db-scripts/003-table-changelog.xml::users_0::Yunus Emre Alpu was: 9:5f8b2fda29b32fbf79ccec4d432ca7ae but is now: 9:4c55cc2069d2cf9960d44a2dcb232085
          db/changelog/db-scripts/003-table-changelog.xml::users_audit_log_0::Yunus Emre Alpu was: 9:8436345a1dc47bc1f3907a8eb5c368ae but is now: 9:f7790a9b17ea70d680701c4e91f32db0
```

You are seeing this error because you **modified existing Liquibase changesets** (`users_0` and `users_audit_log_0`) after they were already applied to your database. Liquibase tracks changeset checksums to prevent accidental or malicious changes to migration history. (In this case table `users` and `users_audit_log` were modified after the initial migration. This table name is just an example, it can be any table name.)

---

## How to fix this error

### **Development Environment (Safe to Reset)**

1. **Clear Liquibase checksums:**
   - Open a terminal in your project root.
   - Run:

     ```bash
        liquibase clearCheckSums
     ```

     or, Gradle users:

     ```bash
        ./gradlew liquibaseClearCheckSums
     ```

2. **Restart your application.**
   - Liquibase will recalculate the checksums and continue.

---

### **Production Environment (Do NOT Edit Existing Changesets!)**

- **Never edit existing changesets in production.**
- Instead, always add a new `<changeSet>` for schema changes.
- If you must fix a checksum mismatch in production, update the `MD5SUM` column in the `DATABASECHANGELOG` table to match the new checksum (not recommended unless you know what you are doing).

```sql
UPDATE DATABASECHANGELOG
SET MD5SUM = '9:4c55cc2069d2cf9960d44a2dcb232085'
WHERE ID = 'users_0' AND AUTHOR = 'Yunus Emre Alpu' AND FILENAME = 'db/changelog/db-scripts/003-table-changelog.xml';

UPDATE DATABASECHANGELOG
SET MD5SUM = '9:f7790a9b17ea70d680701c4e91f32db0'
WHERE ID = 'users_audit_log_0' AND AUTHOR = 'Yunus Emre Alpu' AND FILENAME = 'db/changelog/db-scripts/003-table-changelog.xml';

-- **Important:** This should only be done if you are absolutely sure that the changeset is correct and has been tested. Editing existing changesets in production can lead to data inconsistencies and should be avoided.
```

---

### **Summary**

- For dev: clear checksums and restart.
- For prod: always use new changesets for schema changes.
- Never edit already-applied changesets in production.

---

**Tip:**  
If you are still in development, you can also drop your database and let Liquibase recreate everything from scratch.
