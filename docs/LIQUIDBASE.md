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

> **Note**: You can find more information about Liquidbase in the official documentation [here](https://www.liquidbase.org/documentation/).
