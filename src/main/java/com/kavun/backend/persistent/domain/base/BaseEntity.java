package com.kavun.backend.persistent.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.kavun.constant.base.BaseConstants;

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

  private static final String SEQUENCE_NAME = "kavun_sequence";
  private static final String SEQUENCE_GENERATOR_NAME = "kavun_sequence_generator";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_GENERATOR_NAME)
  @SequenceGenerator(name = SEQUENCE_GENERATOR_NAME, sequenceName = SEQUENCE_NAME, allocationSize = 1)
  private T id;

  @Column(unique = true, nullable = false, updatable = false)
  @NotBlank(message = BaseConstants.PUBLIC_ID_NOT_BLANK)
  private String publicId;

  @Version
  private short version;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CreatedBy
  @Column(nullable = false, updatable = false)
  private String createdBy;

  @LastModifiedDate
  @Column
  private LocalDateTime updatedAt;

  @LastModifiedBy
  @Column
  private String updatedBy;

  @Column
  private LocalDateTime deletedAt;

  @Column
  private String deletedBy;

  @Column(nullable = false)
  private boolean deleted = false;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BaseEntity<?> that) || !that.canEqual(this))
      return false;
    return version == that.version && Objects.equals(publicId, that.publicId);
  }

  protected boolean canEqual(Object other) {
    return other instanceof BaseEntity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, publicId);
  }

  @PrePersist
  protected void onCreate() {
    if (publicId == null) { // Avoid overwriting if set by subclass
      publicId = UUID.randomUUID().toString();
    }
    version = 0;
  }

  @PreRemove
  protected void onDelete() {
    deleted = true;
    deletedAt = LocalDateTime.now();
    deletedBy = new ApplicationAuditorAware().getCurrentAuditor().orElse("system");
  }
}
