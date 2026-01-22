package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.specification.UserSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.constant.CacheConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.UserMapper;
import com.kavun.shared.request.UserRequest;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.enums.RoleType;
import com.kavun.enums.UserHistoryType;
import com.kavun.exception.user.UserAlreadyExistsException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.commons.lang3.Validate;

import lombok.extern.slf4j.Slf4j;

/**
 * This UserService class provides user service operations extending AbstractService.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class UserService extends AbstractService<UserRequest, User, UserDto, UserRepository, UserMapper, UserSpecification> {

  private final Clock clock;
  private final RoleService roleService;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserMapper mapper, UserRepository repository, UserSpecification specification, UserRepository userRepository, Clock clock, RoleService roleService, PasswordEncoder passwordEncoder) {
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
      // If the user exists but has not been verified, then treat this as a new sign-up.
      if (!localUser.isEnabled()) {
        // check if the email in the localUser is the same as the email in userDto,
        // then it's the same account creation being recreated.
        if (localUser.getUsername().equals(userDto.getUsername()) && localUser.getEmail().equals(userDto.getEmail())) {
          LOG.debug(UserConstants.USER_EXIST_BUT_NOT_ENABLED, userDto.getEmail(), localUser);
          return UserUtils.convertToUserDto(localUser);
        }

        // user signed up and could not verify and attempting sign up with either email or username but not both.
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

  /**
   * Returns a user for the given email or null if a user could not be found.
   * Only returns non-deleted users.
   *
   * @param email The email associated to the user to find
   * @return a user for the given email or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Cacheable(CacheConstants.USERS)
  public UserDto findByEmail(final String email) {
    Validate.notNull(email, UserConstants.BLANK_EMAIL);

    User storedUser = repository.findByEmailAndDeletedFalse(email);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  /**
   * Returns a user for the given publicId or null if a user could not be found.
   * Only returns non-deleted users.
   *
   * @param publicId The publicId associated to the user to find
   * @return a user for the given publicId or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Cacheable(CacheConstants.USERS)
  public UserDto findByPublicId(final String publicId) {
    Validate.notNull(publicId, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  /**
   * Find all users that failed to verify their email after a certain time.
   *
   * @return List of users that failed to verify their email.
   */
  public List<UserDto> findAllNotEnabledAfterAllowedDays() {
    var date = LocalDateTime.now(clock).minusDays(UserConstants.DAYS_TO_ALLOW_ACCOUNT_ACTIVATION);
    List<User> expiredUsers = repository.findByEnabledFalseAndCreatedAtBefore(date);

    return UserUtils.convertToUserDto(expiredUsers);
  }

  /**
   * Returns a userDetails for the given username or null if a user could not be found.
   * Only returns non-deleted users for authentication purposes.
   *
   * @param username The username associated to the user to find
   * @return a user for the given username or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  public UserDetails getUserDetails(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    User storedUser = repository.findByUsernameAndDeletedFalse(username);
    return UserDetailsBuilder.buildUserDetails(storedUser);
  }

  /**
   * Checks if the username already exists.
   *
   * @param username the username
   * @return <code>true</code> if username exists
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  public boolean existsByUsername(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);
    return repository.existsByUsernameOrderById(username);
  }

  /**
   * Checks if the username or email already exists and enabled.
   *
   * @param username the username
   * @param email    the email
   * @return <code>true</code> if username exists
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  public boolean existsByUsernameOrEmailAndEnabled(final String username, final String email) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);
    Validate.notNull(email, UserConstants.BLANK_EMAIL);

    return repository.existsByUsernameAndEnabledTrueOrEmailAndEnabledTrueOrderById(username, email);
  }

  /**
   * Validates the username exists and the token belongs to the user with the username.
   *
   * @param username the username
   * @param token    the token
   * @return if token is valid
   */
  public boolean isValidUsernameAndToken(final String username, final String token) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    return repository.existsByUsernameAndVerificationTokenOrderById(username, token);
  }

  /**
   * Update the user with the user instance given and the update type for record.
   *
   * @param userDto         The user with updated information
   * @param userHistoryType the history type to be recorded
   * @return the updated user
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, key = "#userDto.username"),
      @CacheEvict(value = CacheConstants.USERS, key = "#userDto.id"),
      @CacheEvict(value = CacheConstants.USERS, key = "#userDto.email"),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public UserDto updateUser(UserDto userDto, UserHistoryType userHistoryType) {
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);

    userDto.setVerificationToken(null);
    return persistUser(userDto, Collections.emptySet(), userHistoryType, true);
  }

  /**
   * Enables the user by setting the enabled state to true.
   *
   * @param id The user Long
   * @return the updated user or null if user not found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
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

  /**
   * Enables the user by setting the enabled state to true.
   *
   * @param publicId The user publicId
   * @return the updated user or null if user not found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, allEntries = true),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public UserDto enableUser(final String publicId) {
    Validate.notNull(publicId, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
    if (Objects.isNull(storedUser)) {
      LOG.warn("User not found with publicId: {}", publicId);
      return null;
    }

    LOG.debug("Enabling user {}", storedUser.getUsername());
    storedUser.setEnabled(true);
    repository.saveAndFlush(storedUser);

    UserDto userDto = UserUtils.convertToUserDto(storedUser);
    LOG.info("User {} enabled successfully", storedUser.getUsername());
    return userDto;
  }

  /**
   * Disables the user by setting the enabled state to false.
   *
   * @param id The user Long
   * @return the updated user or null if user not found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
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

  /**
   * Disables the user by setting the enabled state to false.
   *
   * @param publicId The user publicId
   * @return the updated user or null if user not found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, allEntries = true),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public UserDto disableUser(final String publicId) {
    Validate.notNull(publicId, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
    if (Objects.isNull(storedUser)) {
      LOG.warn("User not found with publicId: {}", publicId);
      return null;
    }

    LOG.debug("Disabling user {}", storedUser.getUsername());
    storedUser.setEnabled(false);
    repository.saveAndFlush(storedUser);

    UserDto userDto = UserUtils.convertToUserDto(storedUser);
    LOG.info("User {} disabled successfully", storedUser.getUsername());
    return userDto;
  }

  /**
   * Soft delete the user with the user id given by setting deleted flag to true.
   *
   * @param id The Long associated to the user to soft delete
   * @return true if the user was successfully soft deleted, false if user not found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
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

  /**
   * Delete the user with the user id given (Hard Delete).
   *
   * @param id The Long associated to the user to delete
   * @throws NullPointerException in case the given entity is {@literal null}
   */
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

  /**
   * Delete the user with the publicId given (Hard Delete).
   *
   * @param publicId The publicId associated to the user to delete
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USERS, allEntries = true),
      @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
  })
  @Transactional
  public void deleteUser(final String publicId) {
    Validate.notNull(publicId, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = repository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
    if (Objects.nonNull(storedUser)) {
      repository.delete(storedUser);
      LOG.debug("Permanently deleted user with publicId {}", publicId);
    }
  }

  /**
   * Reset the user password with the new password.
   *
   * @param token       the token
   * @param newPassword the new password
   * @throws NullPointerException in case the given entity is {@literal null}
   */
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

  /**
   * Update the password for the user with the id and new password given.
   *
   * @param id          the user Long
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return CustomResponse with status and message
   */
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

  /**
   * Update the password for the user with the publicId and new password given.
   *
   * @param publicId    the user publicId
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return CustomResponse with status and message
   */
  @Transactional
  public String updatePassword(final String publicId, final String oldPassword, final String newPassword) {
    Validate.notNull(publicId, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(oldPassword, UserConstants.BLANK_PASSWORD);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = repository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
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

  /**
   * Generates a secure temporary password.
   */
  public @NonNull String generateSecureTemporaryPassword() {
    String tempPassword = Long.toString(java.util.concurrent.ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
        .replace("-", "").substring(0, 12);
    LOG.debug("Temporary password generated for user");
    return tempPassword;
  }

  /**
   * Updates user password directly without token validation.
   *
   * @param id          the user's Long
   * @param newPassword the new password
   * @return CustomResponse with operation result
   */
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

  /**
   * Updates user password directly without token validation using publicId.
   *
   * @param publicId    the user's publicId
   * @param newPassword the new password
   * @return CustomResponse with operation result
   */
  @Transactional
  public @NonNull Boolean updatePasswordDirectly(@NonNull String publicId, @NonNull String newPassword) {
    Validate.notNull(publicId, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = repository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
    if (storedUser == null) {
      throw new IllegalArgumentException(UserConstants.USER_NOT_FOUND);
    }

    storedUser.setPassword(passwordEncoder.encode(newPassword));
    repository.save(storedUser);

    return true;
  }

  /**
   * Generate unique username for the user.
   *
   * @param firstName the first name
   * @param lastName  the last name
   * @return the unique username
   */
  public String generateUniqueUsername(String firstName, String lastName) {
    Validate.notNull(firstName, UserConstants.BLANK_FIRST_NAME);
    Validate.notNull(lastName, UserConstants.BLANK_LAST_NAME);

    // Remove leading/trailing spaces and special characters, allow Unicode letters/digits
    String baseFirst = firstName.trim().replaceAll("[^\\p{L}\\p{Nd}]", "");
    String baseLast = lastName.trim().replaceAll("[^\\p{L}\\p{Nd}]", "");

    // Fallback if names are empty after cleaning
    if (baseFirst.isEmpty()) baseFirst = "kavun";
    if (baseLast.isEmpty()) baseLast = "user";

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
}
