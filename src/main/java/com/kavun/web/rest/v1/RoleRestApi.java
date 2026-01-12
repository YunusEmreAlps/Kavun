package com.kavun.web.rest.v1;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.service.user.RoleService;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.request.RoleRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
@Tag(name = "03. Role Management", description = "APIs for managing roles")
public class RoleRestApi {

    private final RoleService roleService;

    /**
     * Get all roles with pagination and filtering
     *
     * @param filters search filters (optional)
     * @param pageable pagination parameters
     * @return page of page DTOs
     */
    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieve all roles with pagination and optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved roles")
    })
    public ResponseEntity<Page<RoleDto>> getAll(
            @RequestParam(required = false) Map<String, Object> filters,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<Role> spec = filters != null && !filters.isEmpty()
            ? roleService.search(filters)
            : Specification.where(null);

        Page<RoleDto> roles = roleService.findAll(spec, pageable);
        return ResponseEntity.ok(roles);
    }

    /**
     * Get all roles as list (without pagination)
     *
     * @param filters search filters (optional)
     * @return list of role DTOs
     */
    @GetMapping("/list")
    @Operation(summary = "Get all roles as list", description = "Retrieve all roles without pagination")
    public ResponseEntity<List<RoleDto>> getAllList(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<Role> spec = filters != null && !filters.isEmpty()
            ? roleService.search(filters)
            : Specification.where(null);

        List<RoleDto> roles = roleService.findAll(spec);
        return ResponseEntity.ok(roles);
    }

    /**
     * Get role by ID
     *
     * @param id role ID
     * @return role DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve a specific role by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role found"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleDto> getById(
            @Parameter(description = "Role ID", required = true)
            @PathVariable Long id) {

        RoleDto role = roleService.findById(id);
        return ResponseEntity.ok(role);
    }

    /**
     * Create a new role
     *
     * @param request role creation request
     * @return created role DTO
     */
    @PostMapping
    @Operation(summary = "Create role", description = "Create a new role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Role created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<RoleDto> create(
            @Valid @RequestBody RoleRequest request) {
        RoleDto created = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing role
     *
     * @param id role ID
     * @param request role update request
     * @return updated role DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update an existing role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role updated successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<RoleDto> update(
            @Parameter(description = "Role ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {

        RoleDto updated = roleService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete role by ID
     *
     * @param id role ID
     * @return response entity with no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Soft delete a role (marks as deleted)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Role deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "400", description = "Role already deleted")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Role ID", required = true)
            @PathVariable Long id) {

        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted role
     *
     * @param id role ID
     * @return restored role DTO
     */
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore role", description = "Restore a soft-deleted role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role restored successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "400", description = "Role is not deleted")
    })
    public ResponseEntity<RoleDto> restore(
            @Parameter(description = "Role ID", required = true)
            @PathVariable Long id) {

        RoleDto restored = roleService.restore(id);
        return ResponseEntity.ok(restored);
    }

    /**
     * Check if role exists by ID
     *
     * @param id role ID
     * @return true if exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if role exists", description = "Check if a role exists by ID")
    public ResponseEntity<Boolean> existsById(
            @Parameter(description = "Role ID", required = true)
            @PathVariable Long id) {

        boolean exists = roleService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    /**
     * Count roles with optional filters
     *
     * @param filters search filters (optional)
     * @return number of matching roles
     */
    @GetMapping("/count")
    @Operation(summary = "Count roles", description = "Count roles matching the given filters")
    public ResponseEntity<Long> count(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<Role> spec = filters != null && !filters.isEmpty()
            ? roleService.search(filters)
            : Specification.where(null);

        long count = roleService.count(spec);
        return ResponseEntity.ok(count);
    }

    /**
     * Search roles with dynamic criteria
     *
     * @param searchParams search parameters
     * @param pageable pagination parameters
     * @return page of matching roles
     */
    @PostMapping("/search")
    @Operation(summary = "Search roles", description = "Search roles with dynamic criteria")
    public ResponseEntity<Page<RoleDto>> search(
            @RequestBody Map<String, Object> searchParams,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<Role> spec = roleService.search(searchParams);
        Page<RoleDto> roles = roleService.findAll(spec, pageable);
        return ResponseEntity.ok(roles);
    }
}
