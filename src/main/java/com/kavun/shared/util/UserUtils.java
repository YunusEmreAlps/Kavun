package com.kavun.shared.util;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.domain.user.UserRole;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.backend.service.user.UserService;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.user.ProfileConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.UserMapper;
import com.kavun.shared.request.UserRequest;
import com.kavun.shared.util.core.ValidationUtils;
import com.kavun.web.payload.response.UserResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.datafaker.Faker;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.validator.routines.EmailValidator;

/**
 * User utility class that holds user-related methods used across application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class UserUtils {
  private static final int PASSWORD_MIN_LENGTH = 4;
  public static final int PASSWORD_MAX_LENGTH = 15;

  /** The Constant FAKER. */
  private static final Faker FAKER = new Faker();

  private UserUtils() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }

  /**
   * Gets the UserMapper bean from Spring context.
   *
   * @return the UserMapper instance
   */
  private static UserMapper getUserMapper() {
    return SpringContextHolder.getBean(UserMapper.class);
  }

  /**
   * Gets the UserService bean from Spring context.
   *
   * @return the UserService instance
   */
  private static UserService getUserService() {
    return SpringContextHolder.getBean(UserService.class);
  }

  /**
   * Create a user.
   *
   * @return a user
   */
  public static User createUser() {
    return createUser(FAKER.internet().username());
  }

  // Retrieves the full name of a user based on their ID.
  public static String getUserFullName(Long userId) {
    if (Objects.isNull(userId)) {
      return null;
    }

    UserDto user = getUserService().findById(userId);
    if (Objects.isNull(user)) {
      return null;
    }

    return user.getFirstName() + " " + user.getLastName();
  }

  /**
   * Create a test user with flexibility.
   *
   * @param enabled if the user should be enabled or disabled
   * @return the user
   */
  public static User createUser(final boolean enabled) {
    return createUser(
        FAKER.internet().username(),
        FAKER.internet().password(PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH),
        FAKER.internet().emailAddress(),
        enabled);
  }

  /**
   * Create a user with some flexibility.
   *
   * @param username username used to create user.
   * @param roleType the role type
   * @return a user
   */
  public static User createUser(String username, RoleType roleType) {
    var user = createUser(username);
    user.getUserRoles().add(new UserRole(user, new Role(roleType)));
    return user;
  }

  /**
   * Create a user with some flexibility.
   *
   * @param username username used to create user.
   * @return a user
   */
  public static User createUser(String username) {
    return createUser(
        username,
        FAKER.internet().password(PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH),
        FAKER.internet().emailAddress());
  }

  /**
   * Create a user with some flexibility.
   *
   * @param username username used to create user
   * @param password password used to create user.
   * @param email    email used to create user.
   * @return a user
   */
  public static User createUser(String username, String password, String email) {
    return createUser(username, password, email, false);
  }

  /**
   * Create user with some flexibility.
   *
   * @param username username used to create user.
   * @param password password used to create user.
   * @param email    email used to create user.
   * @param enabled  boolean value used to evaluate if user enabled.
   * @return a user
   */
  public static User createUser(String username, String password, String email, boolean enabled) {
    var user = new User();
    user.setUsername(username);
    user.setPassword(password);
    user.setEmail(email);
    user.setPhone(FAKER.phoneNumber().cellPhone());

    var name = FAKER.name().nameWithMiddle();
    var fullName = name.split(" ");
    user.setFirstName(fullName[0]);
    user.setMiddleName(fullName[1]);
    user.setLastName(fullName[2]);

    if (enabled) {
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);
    }
    return user;
  }

  /**
   * Create a test user with flexibility.
   *
   * @param username the username
   * @return the userDto
   */
  public static UserDto createUserDto(final String username) {
    return UserUtils.convertToUserDto(createUser(username));
  }

  /**
   * Create a test user with flexibility.
   *
   * @param enabled if the user should be enabled or disabled
   * @return the userDto
   */
  public static UserDto createUserDto(final boolean enabled) {
    return createUserDto(FAKER.internet().username(), enabled);
  }

  /**
   * Create a test user with flexibility.
   *
   * @param username the username
   * @param enabled  if the user should be enabled to authenticate
   * @return the userDto
   */
  public static UserDto createUserDto(final String username, boolean enabled) {
    var userDto = UserUtils.convertToUserDto(createUser(username));
    if (enabled) {
      enableUser(userDto);
    }
    return userDto;
  }

  /**
   * Create user with some flexibility.
   *
   * @param username username used to create user.
   * @param password password used to create user.
   * @param email    email used to create user.
   * @param enabled  boolean value used to evaluate if user enabled.
   * @return a userDto
   */
  public static UserDto createUserDto(
      String username, String password, String email, boolean enabled) {
    var user = createUser(username, password, email, enabled);

    return UserUtils.convertToUserDto(user);
  }

  /**
   * Transfers data from entity to transfer object.
   *
   * @param user stored user
   * @return user dto
   */
  public static UserDto convertToUserDto(final User user) {
    var userDto = getUserMapper().toDto(user);
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);
    return userDto;
  }

  // Transfers data from request object to transfer object.
  public static UserDto convertToUserDto(final UserRequest request) {
    var userDto = getUserMapper().toUserDto(request);
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);
    return userDto;
  }

  /**
   * Transfers data from entity to transfer object.
   *
   * @param users stored users
   * @return user dto
   */
  public static List<UserDto> convertToUserDto(final List<User> users) {
    var userDtoList = getUserMapper().toDtoList(users);
    Validate.notNull(userDtoList, UserConstants.USER_DTO_MUST_NOT_BE_NULL);
    return userDtoList;
  }

  /**
   * Transfers data from userDetails to dto object.
   *
   * @param userDetailsBuilder stored user details
   * @return user dto
   */
  public static UserDto convertToUserDto(UserDetailsBuilder userDetailsBuilder) {
    var userDto = getUserMapper().toUserDto(userDetailsBuilder);
    Validate.notNull(userDetailsBuilder, "userDetailsBuilder cannot be null");
    return userDto;
  }

  /**
   * Transfers data from a transfer object to an entity.
   *
   * @param userDto the userDto
   * @return user
   */
  public static User convertToUser(final UserDto userDto) {
    var user = getUserMapper().toUser(userDto);
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);
    return user;
  }

  /**
   * Enables and unlocks a user.
   *
   * @param userDto the userDto
   */
  public static void enableUser(final UserDto userDto) {
    Validate.notNull(userDto, UserConstants.USER_DTO_MUST_NOT_BE_NULL);
    userDto.setEnabled(true);
    userDto.setAccountNonExpired(true);
    userDto.setAccountNonLocked(true);
    userDto.setCredentialsNonExpired(true);
    userDto.setFailedLoginAttempts(0);
  }

  /**
   * Verifies input string is an email.
   *
   * @param email email.
   * @return true if a pattern matches valid3 email, otherwise false.
   */
  public static boolean isEmail(String email) {
    return EmailValidator.getInstance().isValid(email);
  }

  /**
   * Retrieves the roles from the userRoles.
   *
   * @param userRoles the userRoles
   * @return list of the roles as strings
   */
  public static List<String> getRoles(Set<UserRole> userRoles) {
    List<String> roles = new ArrayList<>();

    for (UserRole userRole : userRoles) {
      if (Objects.nonNull(userRole.getRole())) {
        roles.add(userRole.getRole().getName());
      }
    }
    return roles;
  }

  /**
   * Returns the role with the highest precedence if user has multiple roles.
   *
   * @param user the user
   * @return the role
   */
  public static String getTopmostRole(User user) {
    ValidationUtils.validateInputs(user);

    if (Objects.isNull(user.getUserRoles())) {
      return null;
    }

    List<String> roles = getRoles(user.getUserRoles());

    if (roles.contains(RoleType.ROLE_ADMIN.getName())) {
      return RoleType.ROLE_ADMIN.getName();
    }

    return RoleType.ROLE_USER.getName();
  }

  /**
   * Returns the user profile or random image if not found.
   *
   * @param user the user
   * @return profile image
   */
  public static String getUserProfileImage(User user) {
    if (StringUtils.isBlank(user.getProfileImage())) {
      return ProfileConstants.PIC_SUM_PHOTOS_150_RANDOM;
    }

    return user.getProfileImage();
  }

  /**
   * Transfers data from entity to a returnable object.
   *
   * @return user dto
   */
  public static Function<User, UserResponse> getUserResponse() {
    return getUserMapper()::toUserResponse;
  }
}
