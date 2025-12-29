package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.service.user.PermissionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "04. Permissions", description = "APIs for managing permissions")
public class PermissionRestApi {
    private final PermissionService permissionService;

    /**
     * Get all permissions
     *
     * @return list of permissions
     */
    @GetMapping
    public List<Permission> getAll() {
        return permissionService.findAll();
    }

    /**
     * Get a permission by its ID
     *
     * @param id the ID of the permission
     * @return the permission if found, otherwise 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Permission> getById(@PathVariable Long id) {
        return permissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new permission
     *
     * @param permission the permission to create
     * @return the created permission
     */
    @PostMapping
    public Permission create(@RequestBody Permission permission) {
        return permissionService.save(permission);
    }

    /**
     * Delete a permission by its ID
     *
     * @param id the ID of the permission to delete
     * @return 204 No Content if deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
