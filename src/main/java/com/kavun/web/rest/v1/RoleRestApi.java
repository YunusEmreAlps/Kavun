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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "03. Role Management", description = "APIs for managing roles")
public class RoleRestApi {

    private final RoleService roleService;

    /**
     * Get all pages with pagination and filtering
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

        Page<RoleDto> pages = roleService.findAll(spec, pageable);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get all pages as list (without pagination)
     *
     * @param filters search filters (optional)
     * @return list of page DTOs
     */
    @GetMapping("/list")
    @Operation(summary = "Get all roles as list", description = "Retrieve all roles without pagination")
    public ResponseEntity<List<RoleDto>> getAllList(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<Role> spec = filters != null && !filters.isEmpty()
            ? roleService.search(filters)
            : Specification.where(null);

        List<RoleDto> pages = roleService.findAll(spec);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get page by ID
     *
     * @param id page ID
     * @return page DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID", description = "Retrieve a specific permission by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission found"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    public ResponseEntity<RoleDto> getById(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable Long id) {

        RoleDto permission = roleService.findById(id);
        return ResponseEntity.ok(permission);
    }

    /**
     * Create a new page action
     *
     * @param request page creation request
     * @return created page DTO
     */
    @PostMapping
    @Operation(summary = "Create permission", description = "Create a new permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Permission created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<RoleDto> create(
            @Valid @RequestBody RoleRequest request) {
        RoleDto created = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing page
     *
     * @param id page ID
     * @param request page update request
     * @return updated page DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update page action", description = "Update an existing page action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission updated successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<RoleDto> update(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {

        RoleDto updated = roleService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete page by ID
     *
     * @param id page ID
     * @return response entity with no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete permission", description = "Soft delete a permission (marks as deleted)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Permission deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found"),
        @ApiResponse(responseCode = "400", description = "Permission already deleted")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable Long id) {

        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted page
     *
     * @param id page ID
     * @return restored page DTO
     */
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore permission", description = "Restore a soft-deleted permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission restored successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found"),
        @ApiResponse(responseCode = "400", description = "Permission is not deleted")
    })
    public ResponseEntity<RoleDto> restore(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable Long id) {

        RoleDto restored = roleService.restore(id);
        return ResponseEntity.ok(restored);
    }

    /**
     * Check if page exists by ID
     *
     * @param id page ID
     * @return true if exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if page action exists", description = "Check if a page action exists by ID")
    public ResponseEntity<Boolean> existsById(
            @Parameter(description = "Page action ID", required = true)
            @PathVariable Long id) {

        boolean exists = roleService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    /**
     * Count pages with optional filters
     *
     * @param filters search filters (optional)
     * @return number of matching permissions
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
     * Search permissions with dynamic criteria
     *
     * @param searchParams search parameters
     * @param pageable pagination parameters
     * @return page of matching page actions
     */
    @PostMapping("/search")
    @Operation(summary = "Search permissions", description = "Search permissions with dynamic criteria")
    public ResponseEntity<Page<RoleDto>> search(
            @RequestBody Map<String, Object> searchParams,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<Role> spec = roleService.search(searchParams);
        Page<RoleDto> roles = roleService.findAll(spec, pageable);
        return ResponseEntity.ok(roles);
    }
}
