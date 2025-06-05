package com.kavun.backend.service.user;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.kavun.backend.persistent.domain.user.Role;

/**
 * Role service to provide implementation for the definitions about a role.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public interface RoleService {

  /**
   * Create the role with the role instance given.
   *
   * @param role the role
   * @return the persisted role with assigned id
   */
  Role save(final Role role);

  /**
   * Retrieves the role with the specified name.
   *
   * @param name the name of the role to retrieve
   * @return the role tuple that matches the id given
   */
  Role findByName(final String name);

  /**
   * Checks if a role exists by its ID.
   *
   * @param id the ID of the role to check
   * @return true if the role exists, false otherwise
   */
  boolean existById(final Integer id);

  /**
   * Retrieves the role with the specified ID.
   *
   * @param id the ID of the role to retrieve
   * @return the role tuple that matches the ID given
   */
  Role findById(final Long id);

  /**
   * Retrieves the role with the specified public ID.
   *
   * @param publicId the public ID of the role to retrieve
   * @return the role tuple that matches the public ID given
   */
  Role findByPublicId(final String publicId);

  /**
   * Retrieves all roles.
   *
   * @return a list of all roles
   */
  List<Role> findAll();

  /**
   *
   * Retrieves all roles with pagination support.
   *
   * @param pageable the pagination information
   * @return a paginated list of roles
   */
  Page<Role> findAll(Pageable pageable);
}
