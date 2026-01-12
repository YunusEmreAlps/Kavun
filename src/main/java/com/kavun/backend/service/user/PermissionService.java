package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.repository.PermissionRepository;
import com.kavun.backend.persistent.specification.PermissionSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.dto.mapper.PermissionMapper;
import com.kavun.shared.request.PermissionRequest;

import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

/**
* Permission service to provide implementation for the definitions about a permission.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class PermissionService
        extends AbstractService<PermissionRequest, Permission, PermissionDto, PermissionRepository, PermissionMapper, PermissionSpecification> {

    public PermissionService(PermissionMapper mapper, PermissionRepository repository, PermissionSpecification specification) {
        super(mapper, repository, specification);
    }

    @Override
    public Permission mapToEntity(PermissionRequest request) {
        Permission permission = new Permission();
        return permission;
    }

    @Override
    public void updateEntity(Permission entity, PermissionRequest request) {
    }

    public Specification<Permission> specification(Map<String, Object> spec) {
        return specification.search(spec);
    }

    public Specification<Permission> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }

}
