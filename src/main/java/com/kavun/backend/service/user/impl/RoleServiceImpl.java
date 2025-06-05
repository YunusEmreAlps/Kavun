package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.repository.impl.RoleRepository;
import com.kavun.backend.service.user.RoleService;
import com.kavun.constant.CacheConstants;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The RoleServiceImpl class is an implementation for the RoleService Interface.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@RepositoryRestResource(exported = false)
public class RoleServiceImpl implements RoleService {

  private final transient RoleRepository roleRepository;

  /**
   * Create the roleEntity with the roleEntity instance given.
   *
   * @param roleEntity the roleEntity
   * @return the persisted roleEntity with assigned id
   */
  @Override
  @Transactional
  public Role save(Role roleEntity) {
    Validate.notNull(roleEntity, "The roleEntity cannot be null");

    var persistedRole = new Role();
    Role role = findByName(roleEntity.getName());

    // If the roleEntity already exists, merge it with the existing one.
    if(role != null) {
      LOG.info("Role already exists, merging with existing roleEntity {}", roleEntity);
      roleEntity.setId(role.getId());
      roleEntity.setPublicId(role.getPublicId());
      roleEntity.setVersion(role.getVersion());
      persistedRole = roleRepository.saveAndFlush(roleEntity);
      LOG.info("Role merged successfully {}", persistedRole);
    } else {
      LOG.info("Role does not exist, creating new roleEntity {}", roleEntity);
      persistedRole = roleRepository.save(roleEntity);
      LOG.info("Role merged successfully {}", persistedRole);
    }

    return persistedRole;
  }

  /**
   * Retrieves the role with the specified name.
   *
   * @param name the name of the role to retrieve
   * @return the role tuple that matches the id given
   */
  @Override
  @Cacheable(CacheConstants.ROLES)
  public Role findByName(String name) {
    Validate.notNull(name, "The name cannot be null");
    Optional<Role> roleOptional = roleRepository.findFirstByName(name);
    return roleOptional.orElse(null);
  }


  @Override
  @Cacheable(CacheConstants.ROLES)
  public boolean existById(Integer id) {
    Validate.notNull(id, "The Id cannot be null");
    return roleRepository.existsById(id);
  }


  /**
   * Retrieves the role with the specified id.
   * @param id the id of the role to retrieve
   * @return the role tuple that matches the id given
   */
  @Override
  @Cacheable(CacheConstants.ROLES)
  public Role findById(Long id) {
    Validate.notNull(id, "The Id cannot be null");
    Optional<Role> roleOptional = roleRepository.getById(id);
    return roleOptional.orElse(null);
  }

  /**
   * Retrieves the role with the specified public id.
   *
   * @param publicId the public id of the role to retrieve
   * @return the role tuple that matches the public id given
   */
  @Override
  @Cacheable(CacheConstants.ROLES)
  public Role findByPublicId(String publicId) {
    Validate.notNull(publicId, "The publicId cannot be null");
    Optional<Role> roleOptional = roleRepository.findByPublicId(publicId);
    return roleOptional.orElse(null);
  }



  /**
   * Retrieves all roles.
   *
   * @return List of all roles
   */
  @Override
  @Cacheable(CacheConstants.ROLES)
  public List<Role> findAll() {
    LOG.debug("Retrieving all roles");
    return roleRepository.findAll();
  }

  /**
   * Retrieves the all roles with pagination support.
   *
   * @param pageable the pagination information
   * @return a paginated list of roles
   */
  @Override
  @Cacheable(CacheConstants.ROLES)
  public Page<Role> findAll(org.springframework.data.domain.Pageable pageable) {
    Validate.notNull(pageable, "The pageable cannot be null");
    LOG.debug("Retrieving all roles with pagination support: {}", pageable);
    Page<Role> roles = roleRepository.findAll(pageable);
    if (roles.isEmpty()) {
      LOG.debug("No roles found");
    } else {
      LOG.debug("Found {} roles", roles.getTotalElements());
    }
    return roles;
  }
}
