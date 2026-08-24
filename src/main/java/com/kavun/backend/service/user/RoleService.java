package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.repository.UserRoleRepository;
import com.kavun.backend.persistent.specification.RoleSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.mapper.RoleMapper;
import com.kavun.shared.request.RoleRequest;
import com.kavun.web.payload.response.UserRoleResponse;


import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
public class RoleService extends AbstractService<RoleRequest, Role, RoleDto, RoleRepository, RoleMapper, RoleSpecification> {

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;

  public RoleService(RoleMapper mapper, RoleRepository repository, RoleSpecification specification,
      UserRepository userRepository, UserRoleRepository userRoleRepository) {
    super(mapper, repository, specification);
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
  }

  public Specification<Role> search(Map<String, Object> parameterMap) {
    return specification.search(parameterMap);
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

  public void assignRoleToUser(final Long roleId, final Long userId) {
    Role role = repository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found"));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
        .orElseThrow(() -> new IllegalArgumentException("Role not found"));

    // Batch load all users at once - single DB query instead of N queries
    List<User> users = userRepository.findAllById(userIds);

    if (users.size() != userIds.size()) {
      throw new IllegalArgumentException("Some users not found");
    }

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
