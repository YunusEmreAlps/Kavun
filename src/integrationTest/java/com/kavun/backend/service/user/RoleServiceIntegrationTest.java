package com.kavun.backend.service.user;

import com.kavun.IntegrationTestUtils;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.enums.RoleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RoleServiceIntegrationTest extends IntegrationTestUtils {

  @Test
  void saveRole() {
    // Roles are persisted through database seeder class via command line runner.
    // This test is to ensure that a role can be saved again without any errors.
    var persistedRole = roleService.save(new Role(RoleType.ROLE_ADMIN));
    Assertions.assertNotNull(persistedRole);
  }

  @Test
  void getRoleByName() {
    Role storedRole = roleService.findByName(RoleType.ROLE_ADMIN.getName());
    Assertions.assertNotNull(storedRole);
  }
}
