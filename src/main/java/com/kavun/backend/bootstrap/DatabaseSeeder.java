package com.kavun.backend.bootstrap;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.service.user.RoleService;
import com.kavun.backend.service.user.UserService;
import com.kavun.constant.EnvConstants;
import com.kavun.enums.RoleType;
import com.kavun.exception.user.UserAlreadyExistsException;
import com.kavun.shared.util.UserUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * A convenient class to initialize and save user data on application start.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

  private final Environment environment;
  private final UserService userService;
  private final RoleService roleService;

  @Value("${admin.username}")
  private String adminUsername;

  @Value("${admin.email}")
  private String adminEmail;

  @Value("${admin.password}")
  private String adminPassword;

  @Override
  public void run(String... args) {
    Arrays.stream(RoleType.values()).forEach(roleType -> roleService.save(new Role(roleType)));

    // only run these initial data if we are not in test mode.
    if (!Arrays.asList(environment.getActiveProfiles()).contains(EnvConstants.TEST)) {
      persistDefaultAdminUser();
    }
  }

  private void persistDefaultAdminUser() {
    try {
      var adminDto = UserUtils.createUserDto(adminUsername, adminPassword, adminEmail, true);
      Set<RoleType> adminRoleType = Collections.singleton(RoleType.ROLE_ADMIN);

      userService.createUser(adminDto, adminRoleType);
    } catch (UserAlreadyExistsException e) {
      LOG.warn("Admin user already exists!");
    }
  }
}
