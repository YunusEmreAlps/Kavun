# Building an Application on Kavun

Kavun is a library, not a template to fork. The repository has two modules:

| Module | What it is |
| --- | --- |
| `kavun-platform` | The platform: authentication, tenants, users, roles and permissions, the generic CRUD endpoints, auditing, mail, storage, ... All code, its schema, messages and templates. |
| `kavun-app` | Runs the platform on its own (`./gradlew bootRun`, the Docker image): a main class and its `application.properties`. |

An application (say, a jeweler management system) is built like `kavun-app`, in its own
repository, with its own features next to the platform. When the platform improves, the
application takes the new version instead of merging changes.

A complete small example lives in `kavun-platform/src/integrationTest/java/com/example/notes`:
one resource with its table, page and messages. The platform's integration tests run inside it.

## 1. Publish the platform

```bash
./gradlew :kavun-platform:publishToMavenLocal
```

This installs `com.kavun:kavun-platform:<version>` (`version` in `gradle.properties`) in the local
Maven repository, with the versions of every dependency it was built with. For a team, add a
repository (GitHub Packages, Nexus, ...) to the `publishing` block of `kavun-platform/build.gradle`
and to the application's `repositories`.

While working on the platform and an application at the same time, `includeBuild '../Kavun'` in
the application's `settings.gradle` builds the platform from source instead.

## 2. Build file

Use the Spring Boot version of the platform (`build.gradle` of this repository):

```groovy
plugins {
  id 'java'
  id 'org.springframework.boot' version '3.5.12'
  id 'io.spring.dependency-management' version '1.1.7'
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation 'com.kavun:kavun-platform:0.0.1-SNAPSHOT'

  compileOnly 'org.projectlombok:lombok'
  annotationProcessor 'org.projectlombok:lombok'
  annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'

  developmentOnly 'org.springframework.boot:spring-boot-devtools'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
  useJUnitPlatform()
}
```

Spring Web, Security, Data JPA, Validation, Envers, MapStruct and SpringDoc come with the
platform.

## 3. Main class

Put it in the application's own package; its entities, repositories and components go below it.

```java
package com.acme.jeweler;

@SpringBootApplication
@Import(KavunPlatform.class)
public class JewelerApplication {

  public static void main(String[] args) {
    SpringApplication.run(JewelerApplication.class, args);
  }
}
```

`KavunPlatform` adds the platform's components, entities and repositories to the application's.

## 4. Configuration

The platform brings its configuration: `kavun-defaults.properties` and
`kavun-defaults-{profile}.properties` (`development`, `docker`, `test`, `production`) apply below
the application's own files. The application only sets what is its own:

```properties
# src/main/resources/application.properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:development}
spring.application.name=Jeweler
system.name=${SYSTEM_NAME:Jeweler}
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

```properties
# src/main/resources/application-development.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/jeweler
```

A property in `application.properties` overrides the default of every profile, including the
production hardening (actuator endpoints, error details). Override a single profile's value in
`application-{profile}.properties` instead.

## 5. Schema

The application's changelog includes the platform's first:

```xml
<!-- src/main/resources/db/changelog/db.changelog-master.xml -->
<databaseChangeLog ...>
  <include file="db/changelog/kavun-changelog.xml"/>
  <include file="jeweler/001-products.xml" relativeToChangelogFile="true"/>
</databaseChangeLog>
```

Keep the application's changesets in a folder of their own (`db/changelog/jeweler/`): the
platform's live in `db/changelog/changes/`, and a file of the application at the same path would
hide the platform's. `${type.text}` and the conventions of `docs/DATABASE_MIGRATIONS.md` apply.

## 6. Features

Each resource follows `docs/ENDPOINT.md`: entity, changeset, repository, request/DTO/mapper,
service, controller. Its page goes into a `PermissionCatalogContributor` bean of the application;
on start the page is created and granted to every tenant's `ROLE_ADMIN`. The platform's pages are
contributed first, so the application's pages follow the administration pages in the menu until an
administrator reorders them.

## 7. Messages, templates and static files

- `i18n/messages*.properties` are the application's messages. They are resolved before the
  platform's (`i18n/kavun*.properties`), so a key there also replaces a platform message.
- A template, static file or `logback-spring.xml` of the application at the same path as the
  platform's replaces it, e.g. `templates/email/welcome.html` or `templates/error/404.html`.

## Upgrading

Change the platform version. The platform's new changesets run on the next start, before the
application's.
