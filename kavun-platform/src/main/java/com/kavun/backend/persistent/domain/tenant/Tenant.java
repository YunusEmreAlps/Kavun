package com.kavun.backend.persistent.domain.tenant;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A customer of the platform. Its code is stamped on every row it owns (see {@code
 * TenantScopedEntity}) and never changes.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@ToString(callSuper = true)
public class Tenant extends BaseEntity<Long> {

  @Column(nullable = false, unique = true, updatable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  /** Users of a disabled (or deleted) tenant cannot sign in. */
  @Column(nullable = false)
  private boolean enabled = true;

  /** Whether the tenant's users may sign in. */
  public boolean isActive() {
    return enabled && !isDeleted();
  }
}
