package com.kavun.backend.persistent.repository.impl;

import com.kavun.backend.persistent.domain.user.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * The RoleRepository class exposes implementation from JpaRepository on Role
 * entity .
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface RoleRepository extends JpaRepository<Role, Integer> {

  /**
   * Is role exists.
   *
   * @param name name of role.
   * @return boolean.
   */
  boolean existsByName(String name);

  /**
   * Gets role associated with required name.
   *
   * @param name name of role.
   * @return Role found.
   */
  Optional<Role> findFirstByName(String name);

  /**
   * Gets role associated with id.
   *
   * @param id id of role.
   * @return Role found.
   */
  Optional<Role> getById(Long id);

  /**
   * Gets role associated with public id.
   *
   * @param publicId public id of role.
   * @return Role found.
   */
  Optional<Role> findByPublicId(String publicId);

  /**
   * Gets role associated with name.
   *
   * @param name name of role.
   * @return Role found.
   */
  Optional<Role> findByName(String name);
}
