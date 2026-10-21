## All tests are written in JUnit 5.

## Equals, Hashcode and ToString

- Equals and Hashcode is tested using [EqualsVerifier](https://github.com/jqno/equalsverifier)
- Entities never define their own `equals`/`hashCode` (nor Lombok `@EqualsAndHashCode`): `BaseEntity`
  declares them `final`, based on the persistent class and the `publicId` assigned on construction.
  Entity tests therefore use the shared `TestUtils.entityIdentityVerifier(...)`, adding prefab values
  only for self-referencing relations.
- Sample code is

```java
class UserTest {

    @Test
    void equalsContract() {
        User client = UserUtils.createUser();
        User admin = UserUtils.createUser();

        TestUtils.entityIdentityVerifier(User.class)
                .withPrefabValues(User.class, client, admin)
                .verify();
    }
}
```

- ToString is tested using [To String Verifier](https://github.com/jparams/to-string-verifier)
- Sample code is

```java
class UserTest {

    @Test
    void testToString() {
        ToStringVerifier.forClass(User.class)
                .withClassName(NameStyle.SIMPLE_NAME)
                .withIgnoredFields("password", "userRoles", "userHistories")
                .verify();
    }
}
```

## Integration Tests

The integration tests (`kavun-platform/src/integrationTest`) run the platform inside an example
application, `com.example.notes.NotesApplication`, the way a real application uses it: its own
package, a `Note` resource with its own changeset, page and messages, and its own
`application.properties`. `NotesIntegrationTest` checks that these work next to the platform's.

### Default (H2 in memory)

Integration tests can be run in two (2)
profiles which are `integration-test` and `integration-test-ci`

`integration-test` profile can be dynamically activated using the run syntax:

```bash
./gradlew clean build # Profile will be set to integration-test if none is provided
```

Liquibase creates the schema in an H2 in-memory database running in PostgreSQL mode, and Hibernate
validates it against the entities, so a missing changeset fails the build.


### Docker based (Test Container) Postgres Database Integration Test
`integration-test-ci` profile can be dynamically activated using the syntax:

```bash
SPRING_PROFILES_ACTIVE=integration-test-ci ./gradlew clean build
```

This will run the integration test in a docker container using test containers and postgres database.

### Local Amazons3

Tests are powered by [S3 mock library for Java/Scala](https://github.com/findify/s3mock)

Bean definition for Amazons3 in test context is defined as:

```java

@Configuration
@Profile(ProfileTypeConstants.TEST)
public class TestConfig {

    /**
     * A bean to be used by AmazonS3 Service.
     *
     * @param props the aws properties
     * @return instance of AmazonS3Client
     */
    @Bean
    public AmazonS3 amazonS3(AwsProperties props) {
        var endpoint = new EndpointConfiguration(props.getServiceEndpoint(), props.getRegion());
        // Create the credentials provider
        var credentials = new AnonymousAWSCredentials();

        return AmazonS3ClientBuilder.standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpoint)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}

```
