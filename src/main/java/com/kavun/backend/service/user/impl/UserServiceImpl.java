package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.domain.user.UserHistory;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.backend.service.user.RoleService;
import com.kavun.backend.service.user.UserService;
import com.kavun.constant.CacheConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.RoleType;
import com.kavun.enums.UserHistoryType;
import com.kavun.exception.user.UserAlreadyExistsException;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.UserDtoMapper;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.ValidationUtils;
import com.kavun.web.payload.response.CustomResponse;
import com.kavun.web.payload.response.UserResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The UserServiceImpl class provides implementation for the UserService definitions.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private final Clock clock;
  private final RoleService roleService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Saves or updates the user with the user instance given.
   *
   * @param user the user with updated information
   * @param isUpdate if the operation is an update
   * @return the updated user.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Transactional
  public UserDto saveOrUpdate(final User user, final boolean isUpdate) {
    Validate.notNull(user, UserConstants.USER_MUST_NOT_BE_NULL);
    User persistedUser = isUpdate ? userRepository.saveAndFlush(user) : userRepository.save(user);
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
  @Override
  @Transactional
  public @NonNull UserDto createUser(final UserDto userDto) {
    return createUser(userDto, Collections.emptySet());
  }

  /**
   * Create the userDto with the userDto instance given.
   *
   * @param userDto the userDto with updated information
   * @param roleTypes the roleTypes.
   * @return the updated userDto.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Transactional
  public @NonNull UserDto createUser(final UserDto userDto, final Set<RoleType> roleTypes) {
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);

    var localUser = userRepository.findByEmail(userDto.getEmail());
    if (Objects.nonNull(localUser)) {
      // If the user exists but has not been verified, then treat this as a new sign-up.
      if (!localUser.isEnabled()) {
        // check if the email in the localUser is the same as the email ini userDto,
        // then it's the same account creation being recreated.
        if (localUser.getUsername().equals(userDto.getUsername()) && localUser.getEmail().equals(userDto.getEmail())) {
          LOG.debug(UserConstants.USER_EXIST_BUT_NOT_ENABLED, userDto.getEmail(), localUser);
          return UserUtils.convertToUserDto(localUser);
        }

        // user signed up
        // and could not verify and attempting sign up with either email or username but not both.
        LOG.warn("Username or email already exists and either user is using different credentials.");
        throw new UserAlreadyExistsException(UserConstants.USERNAME_OR_EMAIL_EXISTS);
      }

      LOG.warn(UserConstants.USER_ALREADY_EXIST, userDto.getEmail());
      throw new UserAlreadyExistsException(UserConstants.USER_ALREADY_EXIST);
    }

    // Assign a public id to the user. This is used to identify the user in the system and can be
    // shared publicly over the internet.
    userDto.setPublicId(UUID.randomUUID().toString());

    // Update the user password with an encrypted copy of the password
    userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

    return persistUser(userDto, roleTypes, UserHistoryType.CREATED, false);
  }

  @Override
  public Page<UserResponse> findAll(Pageable pageable) {
    Page<User> usersPage = userRepository.findAll(pageable);

    return usersPage.map(UserDtoMapper.MAPPER::toUserResponse);
  }

  @Override
  public Page<UserResponse> findAll(Specification<User> spec, Pageable pageable) {
    return userRepository.findAll(spec, pageable)
        .map(UserDtoMapper.MAPPER::toUserResponse);
  }

  /**
   * Returns users according to the details in the dataTablesInput or null if no user exists.
   *
   * @param dataTablesInput the dataTablesInput
   * @return the dataTablesOutput
   */
  @Override
  public DataTablesOutput<UserResponse> getUsers(final DataTablesInput dataTablesInput) {
    return userRepository.findAll(dataTablesInput, UserUtils.getUserResponse());
  }

  /**
   * Returns a user for the given id or null if a user could not be found.
   *
   * @param id The id associated to the user to find
   * @return a user for the given email or null if a user could not be found.
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  public UserDto findById(final Long id) {
    Validate.notNull(id, UserConstants.USER_ID_MUST_NOT_BE_NULL);

    User storedUser = userRepository.findById(id).orElse(null);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  /**
   * Returns a user for the given publicId or null if a user could not be found.
   *
   * @param publicId the publicId
   * @return the userDto
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Cacheable(CacheConstants.USERS)
  public UserDto findByPublicId(final String publicId) {
    Validate.notNull(publicId, UserConstants.BLANK_PUBLIC_ID);

    User storedUser = userRepository.findByPublicId(publicId);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  /**
   * Returns a user for the given username or null if a user could not be found.
   *
   * @param username The username associated to the user to find
   * @return a user for the given username or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Cacheable(CacheConstants.USERS)
  public UserDto findByUsername(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    var storedUser = userRepository.findByUsername(username);
    if (Objects.isNull(storedUser)) {
      return null;
    }
    return UserUtils.convertToUserDto(storedUser);
  }

  /**
   * Returns a user for the given email or null if a user could not be found.
   *
   * @param email The email associated to the user to find
   * @return a user for the given email or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Cacheable(CacheConstants.USERS)
  public UserDto findByEmail(final String email) {
    Validate.notNull(email, UserConstants.BLANK_EMAIL);

    User storedUser = userRepository.findByEmail(email);
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
  @Override
  public List<UserDto> findAllNotEnabledAfterAllowedDays() {
    var date = LocalDateTime.now(clock).minusDays(UserConstants.DAYS_TO_ALLOW_ACCOUNT_ACTIVATION);
    List<User> expiredUsers = userRepository.findByEnabledFalseAndCreatedAtBefore(date);

    return UserUtils.convertToUserDto(expiredUsers);
  }

  /**
   * Returns a userDetails for the given username or null if a user could not be found.
   *
   * @param username The username associated to the user to find
   * @return a user for the given username or null if a user could not be found
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  public UserDetails getUserDetails(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    User storedUser = userRepository.findByUsername(username);
    return UserDetailsBuilder.buildUserDetails(storedUser);
  }

  /**
   * Checks if the username already exists.
   *
   * @param username the username
   * @return <code>true</code> if username exists
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  public boolean existsByUsername(final String username) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);
    return userRepository.existsByUsernameOrderById(username);
  }

  /**
   * Checks if the username or email already exists and enabled.
   *
   * @param username the username
   * @param email the email
   * @return <code>true</code> if username exists
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  public boolean existsByUsernameOrEmailAndEnabled(final String username, final String email) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);
    Validate.notNull(email, UserConstants.BLANK_EMAIL);

    return userRepository.existsByUsernameAndEnabledTrueOrEmailAndEnabledTrueOrderById(
        username, email);
  }

  /**
   * Validates the username exists and the token belongs to the user with the username.
   *
   * @param username the username
   * @param token the token
   * @return if token is valid
   */
  @Override
  public boolean isValidUsernameAndToken(final String username, final String token) {
    Validate.notNull(username, UserConstants.BLANK_USERNAME);

    return userRepository.existsByUsernameAndVerificationTokenOrderById(username, token);
  }

  /**
   * Update the user with the user instance given and the update type for record.
   *
   * @param userDto The user with updated information
   * @param userHistoryType the history type to be recorded
   * @return the updated user
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Caching(
      evict = {
        @CacheEvict(value = CacheConstants.USERS, key = "#userDto.username"),
        @CacheEvict(value = CacheConstants.USERS, key = "#userDto.publicId"),
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
   * @param publicId The user publicId
   * @return the updated user
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Caching(
      evict = {
        @CacheEvict(value = CacheConstants.USERS),
        @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
      })
  @Transactional
  public UserDto enableUser(final String publicId) {
    Validate.notNull(publicId, UserConstants.BLANK_PUBLIC_ID);

    User storedUser = userRepository.findByPublicId(publicId);
    LOG.debug("Enabling user {}", storedUser);

    if (Objects.nonNull(storedUser)) {
      storedUser.setEnabled(true);
      UserDto userDto = UserUtils.convertToUserDto(storedUser);

      return persistUser(userDto, Collections.emptySet(), UserHistoryType.ACCOUNT_ENABLED, true);
    }
    return null;
  }

  /**
   * Disables the user by setting the enabled state to false.
   *
   * @param publicId The user publicId
   * @return the updated user
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Caching(
      evict = {
        @CacheEvict(value = CacheConstants.USERS),
        @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
      })
  @Transactional
  public UserDto disableUser(final String publicId) {
    Validate.notNull(publicId, UserConstants.BLANK_PUBLIC_ID);

    User storedUser = userRepository.findByPublicId(publicId);
    if (Objects.nonNull(storedUser)) {
      storedUser.setEnabled(false);
      UserDto userDto = UserUtils.convertToUserDto(storedUser);

      return persistUser(userDto, Collections.emptySet(), UserHistoryType.ACCOUNT_DISABLED, true);
    }
    return null;
  }

  /**
   * Delete the user with the user id given.
   *
   * @param publicId The publicId associated to the user to delete
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Caching(
      evict = {
        @CacheEvict(value = CacheConstants.USERS, key = "#publicId"),
        @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
      })
  @Transactional
  public void deleteUser(final String publicId) {
    ValidationUtils.validateInputsWithMessage(UserConstants.BLANK_PUBLIC_ID, publicId);

    // The Number of rows deleted is expected to be 1 since publicId is unique
    int numberOfRowsDeleted = userRepository.deleteByPublicId(publicId);
    LOG.debug("Deleted {} user(s) with publicId {}", numberOfRowsDeleted, publicId);
  }

  /**
   * Transfers user details to a user object then persist to a database.
   *
   * @param userDto the userDto
   * @param roleTypes the roleTypes
   * @param historyType the user history type
   * @param isUpdate if the operation is an update
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
    user.addUserHistory(new UserHistory(UUID.randomUUID().toString(), user, historyType));

    return saveOrUpdate(user, isUpdate);
  }

    /**
   * Reset the user password with the new password.
   *
   * @param token       the token
   * @param newPassword the new password
   * @throws NullPointerException in case the given entity is {@literal null}
   */
  @Override
  @Transactional
  public CustomResponse<String> resetPassword(final String token, final String newPassword) {
    Validate.notNull(token, UserConstants.USER_ID_MUST_NOT_BE_NULL);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = userRepository.findByVerificationToken(token);
    if (Objects.nonNull(storedUser)) {
      storedUser.setPassword(passwordEncoder.encode(newPassword));
      storedUser.setVerificationToken(null);
      userRepository.save(storedUser);
      return CustomResponse.of(
          HttpStatus.OK, null, UserConstants.PASSWORD_RESET_SUCCESSFULLY, null);
    }
    return CustomResponse.of(
        HttpStatus.BAD_REQUEST, null, UserConstants.PASSWORD_RESET_FAILED, null);
  }

  /**
   * Update the password for the user with the publicId and new password given.
   *
   * @param publicId    the publicId
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return CustomResponse with status and message
   */
  @Override
  @Transactional
  public CustomResponse<String> updatePassword(
      final String publicId, final String oldPassword, final String newPassword) {
    Validate.notNull(publicId, UserConstants.BLANK_PUBLIC_ID);
    Validate.notNull(oldPassword, UserConstants.BLANK_PASSWORD);
    Validate.notNull(newPassword, UserConstants.BLANK_PASSWORD);

    User storedUser = userRepository.findByPublicId(publicId);
    if (Objects.isNull(storedUser)) {
      return CustomResponse.of(
          HttpStatus.NOT_FOUND, null, UserConstants.USER_NOT_FOUND, null);
    }

    if (!passwordEncoder.matches(oldPassword, storedUser.getPassword())) {
      return CustomResponse.of(
          HttpStatus.BAD_REQUEST, null, UserConstants.PASSWORD_UPDATED_FAILED, null);
    }

    storedUser.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(storedUser);

    return CustomResponse.of(
        HttpStatus.OK, null, UserConstants.PASSWORD_UPDATED_SUCCESSFULLY, null);
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

    // Remove leading/trailing spaces and special characters, allow Unicode
    // letters/digits
    String baseFirst = firstName.trim().replaceAll("[^\\p{L}\\p{Nd}]", "");
    String baseLast = lastName.trim().replaceAll("[^\\p{L}\\p{Nd}]", "");

    // Fallback if names are empty after cleaning
    if (baseFirst.isEmpty())
      baseFirst = "ziyaret";
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
