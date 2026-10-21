package com.kavun.backend.service.user;

import com.kavun.IntegrationTestUtils;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.enums.RoleType;
import com.kavun.shared.request.RoleRequest;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RoleServiceIntegrationTest extends IntegrationTestUtils {

  @Test
  void createRole() {
    var persistedRole = roleService.create(RoleRequest.builder().name(uniqueRoleName()).build());

    Assertions.assertNotNull(persistedRole.getId());
    Assertions.assertNotNull(persistedRole.getPublicId());
  }

  @Test
  void getRoleByName() {
    // Roles are persisted through database seeder class via command line runner.
    Role storedRole = roleService.findByName(RoleType.ROLE_ADMIN.getName());
    Assertions.assertNotNull(storedRole);
  }

  @Test
  void softDeleteAndRestoreRole() {
    var persistedRole = roleService.create(RoleRequest.builder().name(uniqueRoleName()).build());

    roleService.delete(persistedRole.getId());
    Assertions.assertTrue(roleService.findById(persistedRole.getId()).isDeleted());

    var restoredRole = roleService.restore(persistedRole.getId());
    Assertions.assertFalse(restoredRole.isDeleted());
  }

  private static String uniqueRoleName() {
    return "ROLE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
