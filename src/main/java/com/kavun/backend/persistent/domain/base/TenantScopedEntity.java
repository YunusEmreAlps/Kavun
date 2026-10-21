package com.kavun.backend.persistent.domain.base;

import com.kavun.shared.util.core.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.TenantId;

/**
 * Base class of entities owned by a tenant. Hibernate restricts every query on them to the current
 * tenant ({@link TenantContext}) and stamps the tenant on insert, so services never filter by
 * tenant themselves.
 *
 * <p>Platform-wide data (the permission catalog, tenants themselves) extends {@link BaseEntity}
 * directly.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
@ToString(callSuper = true)
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity<Long> {

  /** The owning tenant's code, set on insert; the column has no default. */
  @TenantId
  @Column(name = "tenant_code", nullable = false, updatable = false, length = 50)
  private String tenantCode;

  @PrePersist
  protected void assignTenant() {
    if (tenantCode != null && !TenantContext.ROOT.equals(tenantCode)) {
      return;
    }
    String tenant = TenantContext.current();
    if (TenantContext.ROOT.equals(tenant)) {
      throw new IllegalStateException(
          getClass().getSimpleName()
              + " created without a tenant; set it explicitly or use TenantContext.runAs");
    }
    tenantCode = tenant;
  }
}
