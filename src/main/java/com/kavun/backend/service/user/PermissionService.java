package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.repository.PermissionRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.dto.mapper.PermissionMapper;
import com.kavun.shared.request.PermissionRequest;


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

    public PermissionService(PermissionMapper mapper, PermissionRepository repository) {
        super(mapper, repository);
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of("entityType", "entityId", "pageAction", "granted", "expiresAt", "createdAt");
    }
}
