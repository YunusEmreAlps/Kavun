package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Role;
import java.util.Optional;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * The RoleRepository class exposes implementation from JpaRepository on Role
 * entity .
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface RoleRepository extends BaseRepository<Role> {

  Optional<Role> findByName(final String name);

  boolean existsById(final Long id);

  long count();

}
