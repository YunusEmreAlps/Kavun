package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import java.util.Collection;
import java.util.Optional;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
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
public interface RoleRepository extends BaseRepository<Role> {

  Optional<Role> findByName(final String name);

  // In the root tenant context: the role of that name in every tenant.
  List<Role> findAllByName(final String name);

  List<Role> findByNameIn(final Collection<String> names);

  boolean existsById(final Long id);

  long count();

  @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur WHERE ur.role.id = :roleId AND ur.deleted = false")
  List<User> getUsersByRoleId(final Long roleId);

}
