package com.kavun;

import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.i18n.I18NService;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.security.AuditService;
import com.kavun.backend.service.security.CookieService;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.backend.service.storage.AmazonS3Service;
import com.kavun.backend.service.user.UserService;
import com.kavun.backend.service.user.RoleService;
import com.kavun.config.properties.AwsProperties;
import com.kavun.config.properties.SystemProperties;
import com.kavun.constant.SecurityConstants;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.task.UserPruningScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.datafaker.Faker;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestUtils {
  protected static final Faker FAKER = new Faker();

  @Autowired protected MockMvc mockMvc;
  @Autowired protected AuditService auditService;
  @Autowired protected AwsProperties awsProperties;
  @Autowired protected AmazonS3Service amazonS3Service;
  @Autowired protected CookieService cookieService;
  @Autowired protected JwtService jwtService;
  @Autowired protected EncryptionService encryptionService;
  @Autowired protected I18NService i18NService;
  @Autowired protected UserService userService;
  @Autowired protected RoleService roleService;
  @Autowired protected UserPruningScheduler userPruningScheduler;
  @Autowired protected SystemProperties systemProperties;
  @Autowired protected EmailService emailService;
  @Autowired protected GreenMail greenMail;
  @Autowired protected UserRepository userRepository;

  @Mock protected MockMultipartFile multipartFile;

  // We are mocking the entire dateTimeProvider since there is only one method in it.
  @MockitoBean protected DateTimeProvider dateTimeProvider;
  // We want to mock just the dateTimeProvider method within the auditHandler
  @MockitoSpyBean protected AuditingHandler auditingHandler;

  /**
   * Creates and verify user with flexible field creation.
   *
   * @param username if a custom username is needed
   * @param enabled if the user should be enabled
   * @return persisted user
   */
  protected UserDto createAndAssertUser(String username, boolean enabled) {
    UserDto userDto = UserUtils.createUserDto(username);
    UserDto persistedUser = persistUser(enabled, userDto);
    Assertions.assertNotNull(persistedUser);
    Assertions.assertNotNull(persistedUser.getId());
    return persistedUser;
  }

  /**
   * Creates and verify user with flexible field creation.
   *
   * @param userDto the userDto
   * @return persisted user
   */
  protected UserDto createAndAssertUser(UserDto userDto) {
    UserDto persistUser = persistUser(userDto.isEnabled(), SerializationUtils.clone(userDto));
    Assertions.assertNotNull(persistUser);
    Assertions.assertNotNull(persistUser.getId());
    Assertions.assertFalse(persistUser.getUserRoles().isEmpty());

    return persistUser;
  }

  /**
   * Creates and verify user with flexible field creation.
   *
   * @param userDto the userDto
   * @return persisted user
   */
  protected UserDto createAndAssertAdmin(UserDto userDto) {
    Set<RoleType> roleTypes = new HashSet<>();

    roleTypes.add(RoleType.ROLE_ADMIN);

    if (userDto.isEnabled()) {
      UserUtils.enableUser(userDto);
    }
    UserDto admin = userService.createUser(SerializationUtils.clone(userDto), roleTypes, false);

    Assertions.assertNotNull(admin);
    Assertions.assertNotNull(admin.getId());
    Assertions.assertFalse(admin.getUserRoles().isEmpty());
    Assertions.assertTrue(
        admin.getUserRoles().stream()
            .anyMatch(role -> role.getRole().getName().equals(RoleType.ROLE_ADMIN.getName())));

    return admin;
  }

  protected UserDto persistUser(boolean enabled, UserDto userDto) {
    Set<RoleType> roleTypes = new HashSet<>();

    roleTypes.add(RoleType.ROLE_USER);

    if (enabled) {
      UserUtils.enableUser(userDto);
    }
    return userService.createUser(userDto, roleTypes, false);
  }

  /**
   * Builds an Authorization header value for the given user, the same way login issues it
   * (encrypted JWT), without going through the rate-limited login endpoint.
   *
   * @param username the username to authenticate as
   * @return the bearer token header value
   */
  protected String bearerTokenFor(String username) {
    var accessToken = encryptionService.encrypt(jwtService.generateJwtToken(username));
    return String.format("%s %s", SecurityConstants.BEARER, accessToken);
  }

  protected MockMultipartFile getMultipartFile(String fileName) {
    return getMultipartFile(fileName, false);
  }

  protected MockMultipartFile getMultipartFile(String fileName, boolean empty) {
    return new MockMultipartFile(
        fileName,
        String.format("%s.png", fileName),
        "image",
        empty ? "".getBytes(StandardCharsets.UTF_8) : fileName.getBytes(StandardCharsets.UTF_8));
  }

  protected static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
