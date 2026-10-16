package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.repository.PermissionRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.security.permission.PermissionCatalog;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.dto.mapper.PermissionMapper;
import com.kavun.shared.request.PermissionRequest;
import com.kavun.shared.util.core.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.util.Set;

/**
* Permission service to provide implementation for the definitions about a permission.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
@Service
@Transactional
public class PermissionService
        extends AbstractService<PermissionRequest, Permission, PermissionDto, PermissionRepository, PermissionMapper> {

    private final PermissionCatalog permissionCatalog;

    public PermissionService(PermissionMapper mapper, PermissionRepository repository,
            PermissionCatalog permissionCatalog) {
        super(mapper, repository);
        this.permissionCatalog = permissionCatalog;
    }

    @Override
    protected void beforeCreate(Permission permission) {
        checkGrantable(permission);
    }

    @Override
    protected void beforeUpdate(Permission permission) {
        checkGrantable(permission);
    }

    // Platform-only pages cannot be granted inside other tenants.
    private void checkGrantable(Permission permission) {
        String page = permission.getPageAction().getPage().getCode();
        if (permissionCatalog.isPlatformOnly(page) && !TenantContext.isPlatform()) {
            throw new AccessDeniedException("Page " + page + " is reserved for the platform");
        }
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of("entityType", "entityId", "pageAction", "granted", "expiresAt", "createdAt");
    }
}
