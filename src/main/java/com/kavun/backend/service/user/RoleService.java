package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.domain.user.UserRole;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.specification.RoleSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.mapper.RoleMapper;
import com.kavun.shared.request.RoleRequest;
import com.kavun.web.payload.response.UserRoleResponse;

import org.springframework.data.jpa.domain.Specification;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;;

/**
 * Role service to provide implementation for the definitions about a role.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class RoleService
        extends AbstractService<RoleRequest, Role, RoleDto, RoleRepository, RoleMapper, RoleSpecification> {

    private final UserRepository userRepository;

    public RoleService(RoleMapper mapper, RoleRepository repository, RoleSpecification specification, UserRepository userRepository) {
        super(mapper, repository, specification);
        this.userRepository = userRepository;
    }

    public Specification<Role> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }

    @Transactional(readOnly = true)
    public Role findByName(String name) {
        LOG.debug("Finding role by name: {}", name);
        Optional<Role> roleOptional = repository.findByName(name);
        return roleOptional.orElse(null);
    }


    public List<UserRoleResponse> getUsersByRoleId(final Long roleId) {
        return mapper.toUserRoleResponseList(repository.getUsersByRoleId(roleId));
    }


    public void assignRoleToUser(final Long roleId, final Long userId) {
    Role role = repository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found"));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // Check if user already has this role
    boolean hasRole = user.getUserRoles().stream()
        .anyMatch(ur -> ur.getRole().getId().equals(roleId));

    if (hasRole) {
      throw new IllegalArgumentException("User already has this role");
    }

    UserRole userRole = new UserRole(user, role);
    user.getUserRoles().add(userRole);
    userRepository.save(user);
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

    // Get existing user IDs that already have this role
    Set<Long> existingUserIds = users.stream()
        .filter(user -> user.getUserRoles().stream()
            .anyMatch(ur -> ur.getRole().getId().equals(roleId)))
        .map(User::getId)
        .collect(Collectors.toSet());

    // Create UserRole entities only for users who don't have the role yet
    users.stream()
        .filter(user -> !existingUserIds.contains(user.getId()))
        .forEach(user -> {
          UserRole userRole = new UserRole(user, role);
          user.getUserRoles().add(userRole);
        });

    // Single batch save operation
    userRepository.saveAll(users);
  }
}
