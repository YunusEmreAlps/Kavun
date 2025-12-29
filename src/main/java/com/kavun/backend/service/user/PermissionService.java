package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Permission;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

/**
* Permission service to provide implementation for the definitions about a permission.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
public interface PermissionService {

    Permission save(@Valid Permission permission);
    Optional<Permission> findById(Long id);
    List<Permission> findAll();
    void deleteById(Long id);
}
