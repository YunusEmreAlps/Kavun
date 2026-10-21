package com.kavun.backend.service.tenant;

import com.kavun.backend.bootstrap.PermissionCatalogSynchronizer;
import com.kavun.backend.persistent.domain.tenant.Tenant;
import com.kavun.backend.persistent.repository.TenantRepository;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.user.RoleService;
import com.kavun.backend.service.user.UserService;
import com.kavun.constant.CacheConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.RoleType;
import com.kavun.exception.user.UserAlreadyExistsException;
import com.kavun.shared.dto.TenantDto;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.TenantMapper;
import com.kavun.shared.request.TenantRequest;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.TenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Manages tenants. Creating a tenant provisions it: its built-in roles, the catalog permissions of
 * its {@code ROLE_ADMIN} and its first administrator, all written as that tenant.
 *
 * <p>Disabling or deleting a tenant locks its users out on their next request. The platform
 * tenant can be neither.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional
public class TenantService
    extends AbstractService<TenantRequest, Tenant, TenantDto, TenantRepository, TenantMapper> {

  private final UserRepository userRepository;
  private final RoleService roleService;
  private final UserService userService;
  private final PermissionCatalogSynchronizer catalogSynchronizer;
  private final Validator validator;
  private final TransactionTemplate transactionTemplate;

  public TenantService(
      TenantMapper mapper,
      TenantRepository repository,
      UserRepository userRepository,
      RoleService roleService,
      UserService userService,
      PermissionCatalogSynchronizer catalogSynchronizer,
      Validator validator,
      PlatformTransactionManager transactionManager) {
    super(mapper, repository);
    this.userRepository = userRepository;
    this.roleService = roleService;
    this.userService = userService;
    this.catalogSynchronizer = catalogSynchronizer;
    this.validator = validator;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  protected Set<String> filterableFields() {
    return Set.of("code", "name", "enabled", "createdAt");
  }

  /**
   * Creates and provisions a tenant. Runs without an outer transaction: the tenant row is saved
   * first, then everything inside the tenant is written in a transaction opened as that tenant. If
   * provisioning fails, the tenant row is removed again.
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public TenantDto create(TenantRequest request) {
    validateCreateRequest(request);
    var violations = validator.validate(request, TenantRequest.Create.class);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
    if (repository.existsByCode(request.getCode())) {
      throw new IllegalArgumentException("Tenant code already exists: " + request.getCode());
    }
    // Usernames and emails are unique across all tenants.
    boolean adminTaken =
        TenantContext.callAs(
            TenantContext.ROOT,
            () ->
                userRepository.existsByUsername(request.getAdminUsername())
                    || userRepository.existsByEmail(request.getAdminEmail()));
    if (adminTaken) {
      throw new UserAlreadyExistsException(UserConstants.USERNAME_OR_EMAIL_EXISTS);
    }

    Tenant tenant = repository.save(mapper.toEntity(request));
    try {
      TenantContext.runAs(
          tenant.getCode(),
          () -> transactionTemplate.executeWithoutResult(status -> provision(request)));
    } catch (RuntimeException e) {
      repository.deleteById(tenant.getId());
      throw e;
    }
    LOG.info("Tenant {} created and provisioned", tenant.getCode());
    return mapper.toDto(tenant);
  }

  @Override
  @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  public TenantDto update(Long id, TenantRequest request) {
    if (TenantContext.PLATFORM.equals(getEntityById(id).getCode())
        && Boolean.FALSE.equals(request.getEnabled())) {
      throw new IllegalStateException("The platform tenant cannot be disabled");
    }
    // The code is stamped on every row of the tenant and never changes.
    request.setCode(null);
    return super.update(id, request);
  }

  @Override
  @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  public void delete(Long id) {
    if (TenantContext.PLATFORM.equals(getEntityById(id).getCode())) {
      throw new IllegalStateException("The platform tenant cannot be deleted");
    }
    super.delete(id);
  }

  @Override
  @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  public TenantDto restore(Long id) {
    return super.restore(id);
  }

  private void provision(TenantRequest request) {
    roleService.ensureBuiltInRoles();
    catalogSynchronizer.grantCatalogTo(roleService.findByName(RoleType.ROLE_ADMIN.getName()));

    UserDto admin =
        UserDto.builder()
            .username(request.getAdminUsername())
            .email(request.getAdminEmail())
            .password(request.getAdminPassword())
            .build();
    UserUtils.enableUser(admin);
    userService.createUser(admin, Set.of(RoleType.ROLE_ADMIN), false);
  }
}
