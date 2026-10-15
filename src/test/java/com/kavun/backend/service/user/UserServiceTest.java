package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.repository.UserRoleRepository;
import com.kavun.backend.persistent.specification.UserSpecification;
import com.kavun.backend.service.security.permission.AccessControlService;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.UserMapper;
import com.kavun.shared.util.UserUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private transient RoleService roleService;

  @Mock private transient UserRepository userRepository;

  @Mock private transient UserRoleRepository userRoleRepository;

  @Mock private transient PasswordEncoder passwordEncoder;

  @Mock private transient AccessControlService accessControlService;

  private final transient Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

  private transient UserService userService;

  private transient UserDto userDto;
  private transient User user;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    userService =
        new UserService(
            Mappers.getMapper(UserMapper.class),
            userRepository,
            new UserSpecification(),
            clock,
            roleService,
            passwordEncoder,
            userRoleRepository,
            accessControlService);
    userDto = UserUtils.createUserDto(testInfo.getDisplayName());
    user = UserUtils.convertToUser(userDto);
  }

  @Test
  void createUserNotExistingWithDefaultRoleAsClient() {
    var role = new Role(RoleType.ROLE_USER);
    role.setId(1L);
    Mockito.when(roleService.findAllByNames(List.of(RoleType.ROLE_USER.name())))
        .thenReturn(List.of(role));
    Mockito.when(userRepository.findByEmail(userDto.getEmail())).thenReturn(null);
    Mockito.when(userRepository.save(ArgumentMatchers.any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(passwordEncoder.encode(ArgumentMatchers.anyString())).thenReturn("encoded");

    var storedUserDetails = userService.createUser(userDto);

    Assertions.assertEquals(userDto.getUsername(), storedUserDetails.getUsername());
    Assertions.assertEquals(RoleType.ROLE_USER.getName(), storedUserDetails.getRole());
  }

  @Test
  void createUserWithNullShouldThrowNullPointerException() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.createUser(null));
  }

  @Test
  void createUserWithNullsShouldThrowNullPointerException() {
    Assertions.assertThrows(
        NullPointerException.class,
        () -> userService.createUser(null, Collections.emptySet(), false));
  }

  @Test
  void getUserByUsername() {
    Mockito.when(userRepository.findByUsernameAndDeletedFalse(userDto.getUsername()))
        .thenReturn(user);

    UserDto storedUserDetails = userService.findByUsername(userDto.getUsername());
    Assertions.assertEquals(userDto.getUsername(), storedUserDetails.getUsername());
  }

  // Test find all users that failed to verify their email after a certain time.
  @Test
  void findAllNotEnabledAfterAllowedDays() {
    var notEnabledUser = UserUtils.createUser();
    Mockito.when(userRepository.findByEnabledFalseAndCreatedAtBefore(ArgumentMatchers.any()))
        .thenReturn(List.of(notEnabledUser));

    List<UserDto> users = userService.findAllNotEnabledAfterAllowedDays();
    Assertions.assertEquals(1, users.size());
    Assertions.assertEquals(notEnabledUser.getUsername(), users.getFirst().getUsername());
  }

  @Test
  void getUserByUsernameWithNullShouldThrowNullPointerException() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.findByUsername(null));
  }

  @Test
  void testGetUserByIdThrowsExceptionOnNullInput() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.findById(null));
  }

  @Test
  void testGetUserByEmailThrowsExceptionOnNullInput() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.findByEmail(null));
  }

  @Test
  void enableUserNotWithNullId() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.enableUser(null));
  }

  @Test
  void disableUserNotWithNullId() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.disableUser(null));
  }

  @Test
  void testDeleteUserThrowsExceptionOnNullInput() {
    Assertions.assertThrows(NullPointerException.class, () -> userService.deleteUser(null));
  }
}
