package com.kavun.backend.service.user;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kavun.IntegrationTestUtils;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.enums.RoleType;
import com.kavun.exception.user.UserAlreadyExistsException;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.request.UserRequest;
import com.kavun.shared.util.UserUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class UserServiceIntegrationTest extends IntegrationTestUtils {

  /**
   * Test attempts to create a user, verify that user has been created successfully by checking
   * assigned id.
   */
  @Test
  void createUserWithoutSpecifiedRoles(TestInfo testInfo) {
    var userDto = UserUtils.createUserDto(testInfo.getDisplayName());
    var persistedUserDto = userService.createUser(userDto);

    Assertions.assertAll(
        () -> {
          Assertions.assertNotNull(persistedUserDto);
          Assertions.assertNotNull(persistedUserDto.getId());
          Assertions.assertEquals(userDto.getUsername(), persistedUserDto.getUsername());

          // assert that the user now has the default USER role assigned after creation.
          Assertions.assertEquals(RoleType.ROLE_USER.getName(), persistedUserDto.getRole());
        });
  }

  /** Creating a user who exists and not enabled should return the existing user. */
  @Test
  void createUserAlreadyExistingAndNotEnabled(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    var existingUser = createAndAssertUser(userDto);

    Assertions.assertEquals(userDto.getId(), existingUser.getId());
  }

  /** Re-registering a not yet enabled email with a different username should fail. */
  @Test
  void createUserAlreadyExistingAndNotEnabledChangedUsername() {
    var userDto = createAndAssertUser(FAKER.internet().username(), false);
    userDto.setUsername("testUsername");

    assertThrows(UserAlreadyExistsException.class, () -> createAndAssertUser(userDto));
  }

  /** Creating a user who exists and is enabled should fail. */
  @Test
  void createUserAlreadyExistingAndEnabled(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), true);

    assertThrows(UserAlreadyExistsException.class, () -> persistUser(true, userDto));
  }

  /** Test checks that an existing user can be retrieved using the username provided. */
  @Test
  void getUserByUsername(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    var storedUser = userService.findByUsername(userDto.getUsername());
    Assertions.assertEquals(userDto.getId(), storedUser.getId());
  }

  @Test
  void getUserByUsernameNotExisting(TestInfo testInfo) {
    Assertions.assertNull(userService.findByUsername(testInfo.getDisplayName()));
  }

  @Test
  void getUserById(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    var storedUser = userService.findById(userDto.getId());
    Assertions.assertEquals(userDto.getUsername(), storedUser.getUsername());
  }

  @Test
  void getUserByIdNotExisting() {
    Assertions.assertNull(userService.findById(Long.MAX_VALUE));
  }

  @Test
  void getUserByEmail(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    var userByEmail = userService.findByEmail(userDto.getEmail());
    Assertions.assertEquals(userDto.getId(), userByEmail.getId());
  }

  @Test
  void getUserByEmailNotExisting(TestInfo testInfo) {
    Assertions.assertNull(userService.findByEmail(testInfo.getDisplayName()));
  }

  @Test
  void findAllNotEnabledAfterCreationDays(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    List<UserDto> users = userService.findAllNotEnabledAfterAllowedDays();
    // User was just created and should not be returned to be deleted.
    Assertions.assertTrue(users.stream().noneMatch(user -> user.getId().equals(userDto.getId())));
  }

  @Test
  void getUserDetails(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    var userDetails = userService.getUserDetails(userDto.getUsername());
    Assertions.assertInstanceOf(UserDetailsBuilder.class, userDetails);
  }

  @Test
  void existsByUsername(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);

    Assertions.assertTrue(userService.existsByUsername(userDto.getUsername()));
  }

  @Test
  void existsByUsernameWithNullThrowsException() {
    assertThrows(NullPointerException.class, () -> userService.existsByUsername(null));
  }

  @Test
  void existsByUsernameNotExisting(TestInfo testInfo) {
    Assertions.assertFalse(userService.existsByUsername(testInfo.getDisplayName()));
  }

  @Test
  void existsByUsernameOrEmailNotEnabled(TestInfo testInfo) {
    var user = createAndAssertUser(testInfo.getDisplayName(), false);

    Assertions.assertFalse(
        userService.existsByUsernameOrEmailAndEnabled(user.getUsername(), user.getEmail()));
  }

  @Test
  void existsByUsernameOrEmailAndEnabled(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), true);

    Assertions.assertTrue(
        userService.existsByUsernameOrEmailAndEnabled(userDto.getUsername(), userDto.getEmail()));
  }

  @Test
  void updateUser(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);
    var newFirstName = FAKER.name().firstName() + "Updated";

    var request = new UserRequest();
    request.setUsername(userDto.getUsername());
    request.setEmail(userDto.getEmail());
    request.setFirstName(newFirstName);
    request.setLastName(userDto.getLastName());
    // updateUser is a full replacement, so required fields must be sent again
    request.setOtpDeliveryMethod(userDto.getOtpDeliveryMethod());

    var updatedUserDto = userService.updateUser(userDto.getId(), request);

    Assertions.assertEquals(userDto.getId(), updatedUserDto.getId());
    Assertions.assertEquals(newFirstName, updatedUserDto.getFirstName());
  }

  @Test
  void enableUser(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), false);
    Assertions.assertFalse(userDto.isEnabled());

    userService.enableUser(userDto.getId());

    Assertions.assertTrue(userService.getUserDetails(userDto.getUsername()).isEnabled());
  }

  @Test
  void enableUserNotExistingThrows() {
    assertThrows(EntityNotFoundException.class, () -> userService.enableUser(Long.MAX_VALUE));
  }

  @Test
  void disableUser(TestInfo testInfo) {
    var userDto = createAndAssertUser(testInfo.getDisplayName(), true);
    Assertions.assertTrue(userDto.isEnabled());

    userService.disableUser(userDto.getId());

    Assertions.assertFalse(userService.getUserDetails(userDto.getUsername()).isEnabled());
  }

  @Test
  void disableUserNotExistingThrows() {
    assertThrows(EntityNotFoundException.class, () -> userService.disableUser(Long.MAX_VALUE));
  }

  @Test
  void isValidUsernameAndTokenWithValidToken() {
    var userDto = createAndAssertUser(UserUtils.createUserDto(false));
    var token = jwtService.generateJwtToken(userDto.getUsername());
    Assertions.assertFalse(userService.isValidUsernameAndToken(userDto.getUsername(), token));

    userDto.setVerificationToken(token);
    userService.saveOrUpdate(UserUtils.convertToUser(userDto), true);

    Assertions.assertTrue(userService.isValidUsernameAndToken(userDto.getUsername(), token));
  }

  @Test
  void isValidUsernameAndTokenWithInvalidToken(TestInfo testInfo) {
    Assertions.assertFalse(
        userService.isValidUsernameAndToken(testInfo.getDisplayName(), testInfo.getDisplayName()));
  }

  @Test
  void isValidUsernameAndTokenWithNullTokenThrowsException(TestInfo testInfo) {
    assertThrows(
        NullPointerException.class,
        () -> userService.isValidUsernameAndToken(null, testInfo.getDisplayName()));
  }

  @Test
  void softDeleteUser() {
    var userDto = createAndAssertUser(UserUtils.createUserDto(false));

    userService.softDeleteUser(userDto.getId());

    Assertions.assertNull(userService.findByUsername(userDto.getUsername()));
  }

  @Test
  void deleteUser() {
    var userDto = createAndAssertUser(UserUtils.createUserDto(false));
    Assertions.assertTrue(userService.existsByUsername(userDto.getUsername()));

    userService.deleteUser(userDto.getId());
    Assertions.assertFalse(userService.existsByUsername(userDto.getUsername()));
  }
}
