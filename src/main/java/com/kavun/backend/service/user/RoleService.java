package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.enums.RoleType;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.repository.UserRoleRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.security.permission.AccessControlService;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.mapper.RoleMapper;
import com.kavun.shared.request.RoleRequest;
import com.kavun.shared.util.UserUtils;
import com.kavun.web.payload.response.UserRoleResponse;

import jakarta.persistence.EntityNotFoundException;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role service to provide implementation for the definitions about a role.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class RoleService extends AbstractService<RoleRequest, Role, RoleDto, RoleRepository, RoleMapper> {

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final AccessControlService accessControlService;

  public RoleService(RoleMapper mapper, RoleRepository repository,
      UserRepository userRepository, UserRoleRepository userRoleRepository,
      AccessControlService accessControlService) {
    super(mapper, repository);
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.accessControlService = accessControlService;
  }

  @Override
  protected Set<String> filterableFields() {
    return Set.of("name", "label", "description", "createdAt");
  }

  /** Creates the built-in roles (ROLE_ADMIN, ROLE_USER) the current tenant does not have yet. */
  public void ensureBuiltInRoles() {
    for (RoleType roleType : RoleType.values()) {
      if (findByName(roleType.getName()) == null) {
        create(RoleRequest.builder()
            .name(roleType.getName())
            .label(roleType.getLabel())
            .description(roleType.getDescription())
            .build());
      }
    }
  }

  // Built-in roles are checked before the request is applied, while the entity still has its
  // original name.
  @Override
  public RoleDto update(Long id, RoleRequest request) {
    accessControlService.checkCanModifyRole(getEntityById(id));
    return super.update(id, request);
  }

  @Override
  public void delete(Long id) {
    accessControlService.checkCanModifyRole(getEntityById(id));
    super.delete(id);
  }

  @Override
  public RoleDto restore(Long id) {
    accessControlService.checkCanModifyRole(getEntityById(id));
    return super.restore(id);
  }

  public Integer count() {
    return (int) repository.count();
  }

  // Custom method to find role by name

  public Role findByName(final String name) {
    return repository.findByName(name).orElse(null);
  }

  public List<Role> findAllByNames(final Collection<String> names) {
    return repository.findByNameIn(names);
  }

  public Role findRoleById(final Long id) {
    return repository.findById(id).orElse(null);
  }

  public List<Role> findAllByIds(final Set<Long> ids) {
    return repository.findAllById(ids);
  }

  public List<Role> findAll() {
    return repository.findAll();
  }

  public List<UserRoleResponse> getUsersByRoleId(final Long roleId) {
    return mapper.toUserRoleResponseList(repository.getUsersByRoleId(roleId));
  }

  // Only administrators may change administrator accounts.
  private void checkCanManage(User user) {
    accessControlService.checkCanManageUserWithRoles(UserUtils.getRoles(user.getUserRoles()));
  }

  public void assignRoleToUser(final Long roleId, final Long userId) {
    Role role = repository.findById(roleId)
        .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    accessControlService.checkCanAssign(role);
    checkCanManage(user);

    // Active check via @SQLRestriction-filtered collection
    boolean alreadyActive = user.getUserRoles().stream()
        .anyMatch(ur -> ur.getRole().getId().equals(roleId));
    if (alreadyActive) {
      throw new IllegalArgumentException("User already has this role");
    }

    // Restore if soft-deleted, otherwise create new
    var existing = userRoleRepository.findByUserIdAndRoleId(userId, roleId);
    if (existing.isPresent()) {
      var ur = existing.get();
      ur.setDeleted(false);
      ur.setDeletedAt(null);
      ur.setDeletedBy(null);
      userRoleRepository.save(ur);
    } else {
      user.addUserRole(role);
      userRepository.save(user);
    }
  }

  public void assignRoleToMultipleUsers(final Long roleId, final List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }

    Role role = repository.findById(roleId)
        .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

    // Batch load all users at once - single DB query instead of N queries
    List<User> users = userRepository.findAllById(userIds);

    if (users.size() != userIds.size()) {
      throw new EntityNotFoundException("Some users not found");
    }
    accessControlService.checkCanAssign(role);
    users.forEach(this::checkCanManage);

    // Get user IDs that already have this role (active only via @SQLRestriction)
    Set<Long> existingUserIds = users.stream()
        .filter(user -> user.getUserRoles().stream()
            .anyMatch(ur -> ur.getRole().getId().equals(roleId)))
        .map(User::getId)
        .collect(Collectors.toSet());

    // For users without the active role: restore soft-deleted or create new
    for (User user : users) {
      if (existingUserIds.contains(user.getId())) {
        continue;
      }
      var existing = userRoleRepository.findByUserIdAndRoleId(user.getId(), roleId);
      if (existing.isPresent()) {
        var ur = existing.get();
        ur.setDeleted(false);
        ur.setDeletedAt(null);
        ur.setDeletedBy(null);
        userRoleRepository.save(ur);
      } else {
        user.addUserRole(role);
      }
    }

    // Batch save users that received new assignments
    userRepository.saveAll(users);
  }
}
