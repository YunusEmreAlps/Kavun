package com.kavun.backend.service.user;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.specification.UserSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.constant.CacheConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.UserMapper;
import com.kavun.shared.request.UserRequest;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.enums.RoleType;
import com.kavun.enums.UserHistoryType;
import com.kavun.exception.user.UserAlreadyExistsException;
import com.kavun.web.payload.request.UserRoleRequest;
import com.kavun.web.payload.response.UserResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.commons.lang3.Validate;

import lombok.extern.slf4j.Slf4j;

/**
 * This UserService class provides user service operations extending
 * AbstractService.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class UserService
    extends AbstractService<UserRequest, User, UserDto, UserRepository, UserMapper, UserSpecification> {

  private final Clock clock;
  private final RoleService roleService;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserMapper mapper, UserRepository repository, UserSpecification specification,
      Clock clock, RoleService roleService, PasswordEncoder passwordEncoder) {
    super(mapper, repository, specification);
    this.clock = clock;
    this.roleService = roleService;
    this.passwordEncoder = passwordEncoder;
  }

  public Specification<User> search(Map<String, Object> paramaterMap) {
    return specification.search(paramaterMap);
  }

  public Integer count() {
    return (int) repository.count();
  }

  /**
   * Saves or updates the user with the user instance given.
   *
   * @param user     the user with updated information
   * @param isUpdate if the operation is an update
   * @return the updated user.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Transactional
  public UserDto saveOrUpdate(final User user, final boolean isUpdate) {
    Validate.notNull(user, UserConstants.USER_MUST_NOT_BE_NULL);
    User persistedUser = isUpdate ? repository.saveAndFlush(user) : repository.save(user);
    LOG.debug(UserConstants.USER_PERSISTED_SUCCESSFULLY, persistedUser);

    return UserUtils.convertToUserDto(persistedUser);
  }

  /**
   * Create the userDto with the userDto instance given.
   *
   * @param userDto the userDto with updated information
   * @return the updated userDto.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Transactional
  public @NonNull UserDto createUser(final UserDto userDto) {
    return createUser(userDto, Collections.emptySet());
  }

  /**
   * Create the userDto with the userDto instance given.
   *
   * @param userDto   the userDto with updated information
   * @param roleTypes the roleTypes.
   * @return the updated userDto.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Transactional
  public @NonNull UserDto createUser(final UserDto userDto, final Set<RoleType> roleTypes) {
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);

    var localUser = repository.findByEmail(userDto.getEmail());
    if (Objects.nonNull(localUser)) {
      // If the user exists but has not been verified, then treat this as a new
      // sign-up.
      if (!localUser.isEnabled()) {
        // check if the email in the localUser is the same as the email in userDto,
        // then it's the same account creation being recreated.
        if (localUser.getUsername().equals(userDto.getUsername()) && localUser.getEmail().equals(userDto.getEmail())) {
          LOG.debug(UserConstants.USER_EXIST_BUT_NOT_ENABLED, userDto.getEmail(), localUser);
          return UserUtils.convertToUserDto(localUser);
        }

        // user signed up and could not verify and attempting sign up with either email
        // or username but not both.
        LOG.warn("Username or email already exists and either user is using different credentials.");
        throw new UserAlreadyExistsException(UserConstants.USERNAME_OR_EMAIL_EXISTS);
      }

      LOG.warn(UserConstants.USER_ALREADY_EXIST, userDto.getEmail());
      throw new UserAlreadyExistsException(UserConstants.USER_ALREADY_EXIST);
    }

    // Update the user password with an encrypted copy of the password
    userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

    return persistUser(userDto, roleTypes, UserHistoryType.CREATED, false);
  }

  public Page<UserResponse> findAll(Pageable pageable) {
    Page<User> usersPage = repository.findAll(pageable);
    return usersPage.map(mapper::toUserResponse);
  }

  public Page<UserDto> findAll(Specification<User> spec, Pageable pageable) {
    return repository.findAll(spec, pageable)
        .map(mapper::toDto);
  }

  /**
   * Returns a user for the given id or null if a user could not be found.
   *
   * @param id The Long associated to the user to find
   * @return a user for the given id or null if a user could not be found.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Cacheable(CacheConstants.USERS)
  public UserDto findById(final Long id) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findById(id).orElse(null);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  /**
   * Returns a user for the given username or null if a user could not be found.
   * Only returns non-deleted users.
   *
   * @param username The username associated to the user to find
   * @return a user for the given username or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Cacheable(CacheConstants.USERS)
  public UserDto findByUsername(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    var storedUser = repository.findByUsernameAndDeletedFalse(username);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  // Returns a user for the given email or null if a user could not be found. Only
  // returns non-deleted users.
  @Cacheable(CacheConstants.USERS)
  public UserDto findByEmail(final String email) {
    Validate.notNull(email, UserConstants.BLANK_EMAIL);

    User storedUser = repository.findByEmailAndDeletedFalse(email);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  // Returns a user for the given phone or null if a user could not be found. Only
  // returns non-deleted users.
  @Cacheable(CacheConstants.USERS)
  public UserDto findByPhone(final String phone) {
    Validate.notNull(phone, UserConstants.BLANK_PHONE);

    User storedUser = repository.findByPhoneAndDeletedFalse(phone);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  // Find all users that failed to verify their email after a certain time.
  public List<UserDto> findAllNotEnabledAfterAllowedDays() {
    var date = LocalDateTime.now(clock).minusDays(UserConstants.DAYS_TO_ALLOW_ACCOUNT_ACTIVATION);
    List<User> expiredUsers = repository.findByEnabledFalseAndCreatedAtBefore(date);

    return UserUtils.convertToUserDto(expiredUsers);
  }

  // This method is used for authentication and should only return non-deleted
  // users. Deleted users should not be able to authenticate.
  public UserDetails getUserDetails(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    User storedUser = repository.findByUsernameAndDeletedFalse(username);
    return UserDetailsBuilder.buildUserDetails(storedUser);
  }

  // Checks if the username already exists.
  public boolean existsByUsername(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);
    return repository.existsByUsernameOrderById(username);
  }

  // Checks if the username or email already exists and enabled.
  public boolean existsByUsernameOrEmailAndEnabled(final String username, final String email) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);
    Validate.notNull(email, UserConstants.BLANK_EMAIL);

    return repository.existsByUsernameAndEnabledTrueOrEmailAndEnabledTrueOrderById(username, email);
  }

  // Validates the username exists and the token belongs to the user with the
  // username.
  public boolean isValidUsernameAndToken(final String username, final String token) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    return repository.existsByUsernameAndVerificationTokenOrderById(username, token);
  }

  /**
   * Updates user with validation and race condition prevention.
   * Also updates roles and locations.
   *
   * @param id      the user id to update
   * @param request the user request with updated data
   * @return updated user DTO
   * @throws IllegalArgumentException if user not found or validation fails
   */
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, key = "#id"),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public UserDto updateUser(Long id, UserRequest request) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(request, "User request must not be null");

    // Fetch user
    User existingUser = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException(UserConstants.USER_NOT_FOUND));

    // Validate uniqueness only if changed
    if (!existingUser.getUsername().equals(request.getUsername())) {
      if (repository.existsByUsernameAndIdNotAndDeletedFalse(request.getUsername(), id)) {
        throw new IllegalArgumentException(UserConstants.EXIST_USERNAME);
      }
    }

    if (!existingUser.getEmail().equals(request.getEmail())) {
      if (repository.existsByEmailAndIdNotAndDeletedFalse(request.getEmail(), id)) {
        throw new IllegalArgumentException(UserConstants.EXIST_EMAIL);
      }
    }

    // Update fields
    existingUser.setUsername(request.getUsername());
    existingUser.setEmail(request.getEmail());
    existingUser.setFirstName(request.getFirstName());
    existingUser.setLastName(request.getLastName());
    existingUser.setPhone(request.getPhone());
    existingUser.setProfileImage(request.getProfileImage());
    existingUser.setEnabled(request.isEnabled());
    existingUser.setAccountNonExpired(request.isAccountNonExpired());
    existingUser.setAccountNonLocked(request.isAccountNonLocked());
    existingUser.setCredentialsNonExpired(request.isCredentialsNonExpired());
    existingUser.setOtpDeliveryMethod(request.getOtpDeliveryMethod());

    var savedUser = repository.save(existingUser);

    // Update roles if provided
    if (request.getRoles() != null && !request.getRoles().isEmpty()) {
      assignRolesToUser(id, request.getRoles());
    }

    return UserUtils.convertToUserDto(savedUser);
  }

  // Enables the user by setting the enabled state to true.
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, key = "#id"),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public UserDto enableUser(final Long id) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findById(id).orElse(null);
    if (Objects.isNull(storedUser)) {
      LOG.warn("User not found with id: {}", id);
      return null;
    }

    LOG.debug("Enabling user {}", storedUser.getUsername());
    storedUser.setEnabled(true);
    repository.saveAndFlush(storedUser);

    UserDto userDto = UserUtils.convertToUserDto(storedUser);
    LOG.info("User {} enabled successfully", storedUser.getUsername());
    return userDto;
  }

  // Disables the user by setting the enabled state to false.
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, key = "#id"),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public UserDto disableUser(final Long id) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findById(id).orElse(null);
    if (Objects.isNull(storedUser)) {
      LOG.warn("User not found with id: {}", id);
      return null;
    }

    LOG.debug("Disabling user {}", storedUser.getUsername());
    storedUser.setEnabled(false);
    repository.saveAndFlush(storedUser);

    UserDto userDto = UserUtils.convertToUserDto(storedUser);
    LOG.info("User {} disabled successfully", storedUser.getUsername());
    return userDto;
  }

  // Soft delete the user with the user id given by setting deleted flag to true.
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, key = "#id"),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public boolean softDeleteUser(final Long id) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findById(id).orElse(null);
    if (Objects.isNull(storedUser)) {
      LOG.warn("User not found with id: {}", id);
      return false;
    }

    var authenticatedUser = SecurityUtils.getAuthenticatedUserDetails();
    if (authenticatedUser != null) {
      storedUser.setDeletedBy(authenticatedUser.getId());
    }
    storedUser.setDeletedAt(LocalDateTime.now(clock));
    storedUser.setDeletedBy(SecurityUtils.getAuthenticatedUserDetails().getId());
    repository.saveAndFlush(storedUser);

    LOG.info("Soft deleted user with id {}", id);
    return true;
  }

  // Delete the user with the user id given (Hard Delete).
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, key = "#id"),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public void deleteUser(final Long id) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    repository.deleteById(id);
    LOG.debug("Permanently deleted user with id {}", id);
  }

  // Reset the user password with the new password.
  @Transactional
  public String resetPassword(final String token, final String newPassword) {
    Validate.notNull(token, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = repository.findByVerificationToken(token);
    if (Objects.nonNull(storedUser)) {
      storedUser.setPassword(passwordEncoder.encode(newPassword));
      storedUser.setVerificationToken(null);
      repository.save(storedUser);
      return UserConstants.PASSWORD_RESET_SUCCESSFULLY;
    }
    throw new IllegalArgumentException(UserConstants.PASSWORD_RESET_FAILED);
  }

  // Update the password for the user with the id and new password given.
  @Transactional
  public String updatePassword(final Long id, final String oldPassword, final String newPassword) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(oldPassword, UserConstants.BLANK_PASSWORD);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = repository.findById(id).orElse(null);
    if (Objects.isNull(storedUser)) {
      throw new IllegalArgumentException(UserConstants.USER_NOT_FOUND);
    }

    if (!passwordEncoder.matches(oldPassword, storedUser.getPassword())) {
      throw new IllegalArgumentException(UserConstants.PASSWORD_UPDATED_FAILED);
    }

    storedUser.setPassword(passwordEncoder.encode(newPassword));
    repository.save(storedUser);

    return UserConstants.PASSWORD_UPDATED_SUCCESSFULLY;
  }

  // Updates user password directly without token validation.
  @Transactional
  public @NonNull Boolean updatePasswordDirectly(@NonNull Long id, @NonNull String newPassword) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = repository.findById(id).orElse(null);
    if (storedUser == null) {
      throw new IllegalArgumentException(UserConstants.USER_NOT_FOUND);
    }

    storedUser.setPassword(passwordEncoder.encode(newPassword));
    repository.save(storedUser);

    return true;
  }

  public List<UserResponse> findAllUsers() {
    List<User> usersPage = repository.findAll();
    return usersPage.stream().map(mapper::toUserResponse).toList();
  }

  /**
   * Transfers user details to a user object then persist to a database.
   *
   * @param userDto     the userDto
   * @param roleTypes   the roleTypes
   * @param historyType the user history type
   * @param isUpdate    if the operation is an update
   * @return the userDto
   */
  private UserDto persistUser(
      final UserDto userDto,
      final Set<RoleType> roleTypes,
      final UserHistoryType historyType,
      final boolean isUpdate) {

    // If no role types are specified, then set the default role type
    var localRoleTypes = new HashSet<>(roleTypes);
    if (localRoleTypes.isEmpty() && !isUpdate) {
      localRoleTypes.add(RoleType.ROLE_USER);
    }

    var user = UserUtils.convertToUser(userDto);
    for (RoleType roleType : localRoleTypes) {
      var storedRole = roleService.findByName(roleType.name());
      user.addUserRole(storedRole);
    }
    // user.addUserHistory(new UserHistory(user, historyType));

    return saveOrUpdate(user, isUpdate);
  }

  public User getEntity(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı"));
  }

  /**
   * Assigns multiple roles to a user. Replaces existing roles.
   *
   * @param userId the user id
   * @param roleRequests the list of role requests containing role IDs
   */
  @Transactional
  public void assignRolesToUser(Long userId, List<UserRoleRequest> roleRequests) {
    User user = repository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException(UserConstants.USER_NOT_FOUND));

    // Get requested role IDs
    Set<Long> requestedRoleIds = new HashSet<>();
    if (roleRequests != null && !roleRequests.isEmpty()) {
      roleRequests.forEach(req -> requestedRoleIds.add(req.getRoleId()));
    }

    // Get existing role IDs
    Set<Long> existingRoleIds = new HashSet<>();
    user.getUserRoles().forEach(userRole -> existingRoleIds.add(userRole.getRole().getId()));

    // Remove roles that are no longer requested
    user.getUserRoles().removeIf(userRole -> !requestedRoleIds.contains(userRole.getRole().getId()));

    // Flush deletions to database
    repository.saveAndFlush(user);

    // Add new roles that don't already exist
    for (UserRoleRequest roleRequest : roleRequests) {
      if (!existingRoleIds.contains(roleRequest.getRoleId())) {
        RoleDto roleDto = roleService.findById(roleRequest.getRoleId());
        if (roleDto == null) {
          LOG.warn("Role not found with id: {}", roleRequest.getRoleId());
          continue;
        }
        Role role = new Role();
        role.setId(roleDto.getId());
        role.setName(roleDto.getName());
        user.addUserRole(role);
      }
    }

    repository.save(user);
    LOG.info("Updated roles for user {} - added: {}, removed: {}",
        userId,
        requestedRoleIds.size() - existingRoleIds.stream().filter(requestedRoleIds::contains).count(),
        existingRoleIds.size() - existingRoleIds.stream().filter(requestedRoleIds::contains).count());
  }

  // Generates a secure temporary password.
  public @NonNull String generateSecureTemporaryPassword() {
    String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    LOG.debug("Temporary password generated for user");
    return tempPassword;
  }

  // Generate unique username for the user.
  public String generateUniqueUsername(String firstName, String lastName) {
    Validate.notNull(firstName, UserConstants.BLANK_FIRST_NAME);
    Validate.notNull(lastName, UserConstants.BLANK_LAST_NAME);

    // Remove leading/trailing spaces and special characters
    String baseFirst = firstName.trim().replaceAll("[^\\p{L}\\p{Nd}]", "");
    String baseLast = lastName.trim().replaceAll("[^\\p{L}\\p{Nd}]", "");

    // Fallback if names are empty after cleaning
    if (baseFirst.isEmpty())
      baseFirst = "kavun";
    if (baseLast.isEmpty())
      baseLast = "user";

    // Reserve 1 char for the dot
    int maxFirst = Math.min(baseFirst.length(), UserConstants.USERNAME_MAX_SIZE / 2);
    int maxLast = Math.min(baseLast.length(), UserConstants.USERNAME_MAX_SIZE - maxFirst - 1);

    // Truncate to fit within 50 chars (including dot)
    baseFirst = baseFirst.substring(0, Math.min(baseFirst.length(), maxFirst));
    baseLast = baseLast.substring(0, Math.min(baseLast.length(), maxLast));

    String baseUsername = (baseFirst + "." + baseLast).toLowerCase();
    String username = baseUsername;
    int counter = 1;

    // If username exceeds max length, truncate
    if (username.length() > UserConstants.USERNAME_MAX_SIZE) {
      username = username.substring(0, UserConstants.USERNAME_MAX_SIZE);
    }

    // Ensure uniqueness and length with counter
    while (existsByUsername(username)) {
      String suffix = String.valueOf(counter);
      int trimLength = UserConstants.USERNAME_MAX_SIZE - suffix.length();
      String trimmedBase = baseUsername.length() > trimLength
          ? baseUsername.substring(0, trimLength)
          : baseUsername;
      username = trimmedBase + suffix;
      counter++;
    }
    return username;
  }
}
