package com.kavun.web.rest.v1;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.tenant.TenantService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.TenantDto;
import com.kavun.shared.request.TenantRequest;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant management, for platform administrators only (TENANTS is a platform-only page). Creating
 * a tenant also creates its roles and first administrator.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/tenant")
@Tag(name = "10. Tenant Management", description = "APIs for managing tenants")
@RequirePermission(page = Pages.TENANTS)
public class TenantRestApi extends AbstractCrudRestApi<TenantRequest, TenantDto> {

  public TenantRestApi(TenantService service) {
    super(service);
  }
}
