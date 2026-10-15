package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.user.RoleService;
import com.kavun.constant.PermissionConstants.Actions;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.request.RoleRequest;
import com.kavun.web.payload.response.UserRoleResponse;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for roles, plus role assignment.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/role")
@Tag(name = "03. Role Management", description = "APIs for managing roles")
@RequirePermission(page = Pages.ROLES)
public class RoleRestApi extends AbstractCrudRestApi<RoleRequest, RoleDto> {

  private final RoleService roleService;

  public RoleRestApi(RoleService roleService) {
    super(roleService);
    this.roleService = roleService;
  }

  @Loggable
  @GetMapping("/{id}/users")
  @Operation(summary = "List the users holding a role")
  public List<UserRoleResponse> getUsersByRoleId(@PathVariable Long id) {
    return roleService.getUsersByRoleId(id);
  }

  @Loggable
  @RequirePermission(action = Actions.ASSIGN)
  @PostMapping("/{roleId}/assign-users")
  @Operation(summary = "Assign a role to several users")
  public void assignRoleToMultipleUsers(
      @PathVariable Long roleId, @RequestBody List<Long> userIds) {
    roleService.assignRoleToMultipleUsers(roleId, userIds);
  }

  @Loggable
  @RequirePermission(action = Actions.ASSIGN)
  @PostMapping("/assign-roles/{userId}")
  @Operation(summary = "Assign several roles to a user")
  public void assignMultipleRolesToUser(
      @PathVariable Long userId, @RequestBody List<Long> roleIds) {
    for (Long roleId : roleIds) {
      roleService.assignRoleToUser(roleId, userId);
    }
  }
}
