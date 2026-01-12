package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.persistent.specification.RoleSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.mapper.RoleMapper;
import com.kavun.shared.request.RoleRequest;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.jpa.domain.Specification;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;;

/**
 * Role service to provide implementation for the definitions about a role.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class RoleService
        extends AbstractService<RoleRequest, Role, RoleDto, RoleRepository, RoleMapper, RoleSpecification> {

    public RoleService(RoleMapper mapper, RoleRepository repository, RoleSpecification specification) {
        super(mapper, repository, specification);
    }

    @Override
    public Role mapToEntity(RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setLabel(request.getLabel());
        role.setDescription(request.getDescription());
        return role;
    }

    @Override
    public void updateEntity(Role entity, RoleRequest request) {
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getLabel() != null) {
            entity.setLabel(request.getLabel());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }

    public Specification<Role> specification(Map<String, Object> spec) {
        return specification.search(spec);
    }

    public Specification<Role> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }

    @Transactional(readOnly = true)
    public Role findByName(String name) {
        LOG.debug("Finding role by name: {}", name);
        return repository.findByName(name)
                .orElseThrow(() -> {
                    LOG.error("Role not found with name: {}", name);
                    return new EntityNotFoundException("Role bulunamadı: " + name);
                });
    }

}
