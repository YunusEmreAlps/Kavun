package com.kavun.web.rest.v1;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.service.user.RoleService;
import com.kavun.constant.AdminConstants;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * This class handles all rest calls for managing roles in the system.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "03. Role Management", description = "APIs for managing roles")
@RequestMapping(AdminConstants.API_V1_ROLE_ROOT_URL)
public class RoleRestApi {

    private final RoleService roleService;

    private static final String AUTHORIZE = "isFullyAuthenticated() && hasRole(T(com.kavun.enums.RoleType).ROLE_ADMIN)";

    /**
     * Get all roles.
     *
     * @return List of roles
     */
    @Loggable
    @PreAuthorize(AUTHORIZE)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Role>> getAllRoles() {
        // Logic to retrieve all roles
        // This is just a placeholder, implement the actual logic
        List<Role> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }

    /**
     * Get role by ID.
     *
     * @param id the ID of the role to retrieve
     * @return the role with the specified ID
     */
    @Loggable
    @PreAuthorize(AUTHORIZE)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Role> getRoleById(Long id) {
        Role role = roleService.findById(id);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    /**
     * Get role by public ID.
     *
     * @param publicId the public ID of the role to retrieve
     * @return the role with the specified public ID
     */
    @Loggable
    @PreAuthorize(AUTHORIZE)
    @GetMapping(value = "/public/{publicId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Role> getRoleByPublicId(String publicId) {
        Role role = roleService.findByPublicId(publicId);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    /**
     * Get role by name.
     *
     * @param name the name of the role to retrieve
     * @return the role with the specified name
     */
    @Loggable
    @PreAuthorize(AUTHORIZE)
    @GetMapping(value = "/name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Role> getRoleByName(String name) {
        Role role = roleService.findByName(name);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    /**
     * Get all pageable roles.
     *
     * @param pageable the pagination information
     * @return List of roles
     */
    @Loggable
    @PreAuthorize(AUTHORIZE)
    @GetMapping(value = "/pageable", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Role>> getAllRolesPageable(Pageable pageable) {
        // Logic to retrieve all roles with pagination
        // This is just a placeholder, implement the actual logic
        Page<Role> roles = roleService.findAll(pageable);
        return ResponseEntity.ok(roles);
    }
}
