package com.kavun.backend.persistent.domain.base;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.kavun.TestUtils;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.ToString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Test class for the BaseEntity.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
class BaseEntityTest {

  @Test
  void testBaseEntityWithString(TestInfo testInfo) {
    var baseEntity = createBaseEntity(testInfo.getDisplayName());

    Assertions.assertAll(
        () -> {
          Assertions.assertEquals(testInfo.getDisplayName(), baseEntity.getId());
          Assertions.assertNull(baseEntity.getVersion());
        });
  }

  @Test
  void testBaseEntityWithLong() {
    var id = 1L;
    var baseEntity = createBaseEntity(id);

    Assertions.assertEquals(id, baseEntity.getId());
  }

  @Test
  void equalsContract() {
    TestUtils.entityIdentityVerifier(TestableBaseEntity.class).verify();
  }

  @Test
  void newEntitiesHaveDistinctIdentity() {
    var first = new TestableBaseEntity<Long>();
    var second = new TestableBaseEntity<Long>();

    Assertions.assertNotNull(first.getPublicId());
    Assertions.assertNotEquals(first, second);
  }

  @Test
  void identityIsStableWhenDatabaseIdIsAssigned() {
    var entity = new TestableBaseEntity<Long>();
    var hashCodeBeforeSave = entity.hashCode();

    entity.setId(42L);

    Assertions.assertEquals(hashCodeBeforeSave, entity.hashCode());
  }

  @Test
  void entitiesOfDifferentTypesAreNeverEqual() {
    var entity = new TestableBaseEntity<Long>();
    var other = new OtherTestableBaseEntity();
    other.setPublicId(entity.getPublicId());

    Assertions.assertNotEquals(entity, other);
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(TestableBaseEntity.class).withClassName(NameStyle.SIMPLE_NAME).verify();
  }

  private <T extends Serializable> BaseEntity<T> createBaseEntity(T id) {
    var baseEntity = new TestableBaseEntity<T>();
    baseEntity.setId(id);
    baseEntity.setCreatedBy(1L);
    baseEntity.setUpdatedBy(baseEntity.getCreatedBy());
    baseEntity.setPublicId(UUID.randomUUID().toString());
    baseEntity.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
    baseEntity.setUpdatedAt(LocalDateTime.now(Clock.systemUTC()));

    return baseEntity;
  }

  /** A second entity type, used to check that identity does not cross entity types. */
  private static class OtherTestableBaseEntity extends BaseEntity<Long> {
    // No additional implementation needed
  }
}

/** Concrete implementation of BaseEntity for testing purposes. */
@ToString(callSuper = true)
class TestableBaseEntity<T extends Serializable> extends BaseEntity<T> {
  // No additional implementation needed
}
