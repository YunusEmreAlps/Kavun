package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.tenant.Tenant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repository for tenants.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface TenantRepository extends BaseRepository<Tenant> {

  Optional<Tenant> findByCode(String code);

  boolean existsByCode(String code);
}
