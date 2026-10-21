package com.kavun.backend.persistent.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


/**
 * BaseEntity class allows an entity to inherit common properties from it.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
@ToString
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<T extends Serializable> {
  // private static final String SEQUENCE_NAME = "kavun_sequence";
  // private static final String SEQUENCE_GENERATOR_NAME =
  // "kavun_sequence_generator";

  @Id
  // @GeneratedValue(strategy = GenerationType.SEQUENCE, generator =
  // SEQUENCE_GENERATOR_NAME)
  // @SequenceGenerator(name = SEQUENCE_GENERATOR_NAME, sequenceName =
  // SEQUENCE_NAME, allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JdbcTypeCode(SqlTypes.BIGINT)
  private T id;

  /**
   * Assigned on construction (not on persist) so that equals/hashCode are stable for the whole
   * lifecycle of the entity, including before it is saved.
   */
  @Column(unique = true, nullable = false, updatable = false)
  private String publicId = UUID.randomUUID().toString();

  @Version
  @Column(nullable = false)
  private Integer version;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CreatedBy
  @Column(nullable = false, updatable = false)
  private Long createdBy;

  @LastModifiedDate
  @Column
  private LocalDateTime updatedAt;

  @LastModifiedBy
  @Column
  private Long updatedBy;

  @Column
  private LocalDateTime deletedAt;

  @Column
  private Long deletedBy;

  @Column(nullable = false)
  private boolean deleted = false;

  @Column
  private Integer orderIndex = 0;

  @PrePersist
  protected void generatePublicId() {
    if (this.publicId == null) {
      this.publicId = UUID.randomUUID().toString();
    }
  }

  /**
   * Entity identity is the publicId of the same persistent class. Final so that every entity
   * shares one consistent contract; getters are used so Hibernate proxies resolve correctly.
   */
  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseEntity<?> other) || persistentClass(this) != persistentClass(other)) {
      return false;
    }
    return Objects.equals(getPublicId(), other.getPublicId());
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(getPublicId());
  }

  private static Class<?> persistentClass(Object entity) {
    return entity instanceof HibernateProxy proxy
        ? proxy.getHibernateLazyInitializer().getPersistentClass()
        : entity.getClass();
  }
}
