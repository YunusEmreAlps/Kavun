package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.user.PermissionService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.request.PermissionRequest;
import com.kavun.web.payload.request.RolePermissionsRequest;
import com.kavun.web.payload.response.RolePermissionMatrixResponse;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for permissions, plus the permission matrix of a role.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/permission")
@Tag(name = "07. Permission Management", description = "APIs for managing permissions")
@RequirePermission(page = Pages.PERMISSIONS)
public class PermissionRestApi extends AbstractCrudRestApi<PermissionRequest, PermissionDto> {

  private final PermissionService permissionService;

  public PermissionRestApi(PermissionService service) {
    super(service);
    this.permissionService = service;
  }

  @Loggable
  @GetMapping("/role/{roleId}")
  @Operation(summary = "The permission matrix of a role")
  public RolePermissionMatrixResponse getRoleMatrix(@PathVariable Long roleId) {
    return permissionService.getRoleMatrix(roleId);
  }

  @Loggable
  @PutMapping("/role/{roleId}")
  @Operation(summary = "Grant a role exactly the given page actions of its permission matrix")
  public RolePermissionMatrixResponse updateRoleMatrix(
      @PathVariable Long roleId, @Valid @RequestBody RolePermissionsRequest request) {
    return permissionService.updateRoleMatrix(roleId, request.pageActionIds());
  }
}
