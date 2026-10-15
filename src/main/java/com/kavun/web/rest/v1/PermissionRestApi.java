package com.kavun.web.rest.v1;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.user.PermissionService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.request.PermissionRequest;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for permissions.
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

  public PermissionRestApi(PermissionService service) {
    super(service);
  }
}
