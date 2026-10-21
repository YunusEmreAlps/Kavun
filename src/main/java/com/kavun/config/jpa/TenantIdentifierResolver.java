package com.kavun.config.jpa;

import com.kavun.shared.util.core.TenantContext;
import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Hands {@link TenantContext} to Hibernate, which then filters every query on entities with a
 * {@code @TenantId} field and fills that field on insert. {@link TenantContext#ROOT} disables the
 * filter.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class TenantIdentifierResolver
    implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

  @Override
  public String resolveCurrentTenantIdentifier() {
    return TenantContext.current();
  }

  @Override
  public boolean isRoot(String tenantId) {
    return TenantContext.ROOT.equals(tenantId);
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
  }
}
