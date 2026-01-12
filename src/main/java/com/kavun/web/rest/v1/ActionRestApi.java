package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.service.user.ActionService;
import com.kavun.shared.dto.ActionDto;
import com.kavun.shared.request.ActionRequest;

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
@RequestMapping("/api/v1/action")
@RequiredArgsConstructor
@Tag(name = "04. Action Management", description = "APIs for managing actions")
public class ActionRestApi {

    private final ActionService actionService;

    /**
     * Get all actions with pagination and filtering
     *
     * @param filters search filters (optional)
     * @param pageable pagination parameters
     * @return page of action DTOs
     */
    @GetMapping
    @Operation(summary = "Get all actions", description = "Retrieve all actions with pagination and optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved actions")
    })
    public ResponseEntity<Page<ActionDto>> getAll(
            @RequestParam(required = false) Map<String, Object> filters,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<Action> spec = filters != null && !filters.isEmpty()
            ? actionService.search(filters)
            : Specification.where(null);

        Page<ActionDto> actions = actionService.findAll(spec, pageable);
        return ResponseEntity.ok(actions);
    }

    /**
     * Get all actions as list (without pagination)
     *
     * @param filters search filters (optional)
     * @return list of action DTOs
     */
    @GetMapping("/list")
    @Operation(summary = "Get all actions as list", description = "Retrieve all actions without pagination")
    public ResponseEntity<List<ActionDto>> getAllList(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<Action> spec = filters != null && !filters.isEmpty()
            ? actionService.search(filters)
            : Specification.where(null);

        List<ActionDto> actions = actionService.findAll(spec);
        return ResponseEntity.ok(actions);
    }

    /**
     * Get action by ID
     *
     * @param id action ID
     * @return action DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get action by ID", description = "Retrieve a specific action by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Action found"),
        @ApiResponse(responseCode = "404", description = "Action not found")
    })
    public ResponseEntity<ActionDto> getById(
            @Parameter(description = "Action ID", required = true)
            @PathVariable Long id) {

        ActionDto action = actionService.findById(id);
        return ResponseEntity.ok(action);
    }

    /**
     * Create a new action
     *
     * @param request action creation request
     * @return created action DTO
     */
    @PostMapping
    @Operation(summary = "Create action", description = "Create a new action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Action created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ActionDto> create(
            @Valid @RequestBody ActionRequest request) {

        ActionDto created = actionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing action
     *
     * @param id action ID
     * @param request action update request
     * @return updated action DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update action", description = "Update an existing action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Action updated successfully"),
        @ApiResponse(responseCode = "404", description = "Action not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ActionDto> update(
            @Parameter(description = "Action ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ActionRequest request) {

        ActionDto updated = actionService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete action by ID
     *
     * @param id action ID
     * @return response entity with no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete action", description = "Soft delete an action (marks as deleted)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Action deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Action not found"),
        @ApiResponse(responseCode = "400", description = "Action already deleted")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Action ID", required = true)
            @PathVariable Long id) {

        actionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted action
     *
     * @param id action ID
     * @return restored action DTO
     */
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore action", description = "Restore a soft-deleted action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Action restored successfully"),
        @ApiResponse(responseCode = "404", description = "Action not found"),
        @ApiResponse(responseCode = "400", description = "Action is not deleted")
    })
    public ResponseEntity<ActionDto> restore(
            @Parameter(description = "Action ID", required = true)
            @PathVariable Long id) {

        ActionDto restored = actionService.restore(id);
        return ResponseEntity.ok(restored);
    }

    /**
     * Check if action exists by ID
     *
     * @param id action ID
     * @return true if exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if action exists", description = "Check if an action exists by ID")
    public ResponseEntity<Boolean> existsById(
            @Parameter(description = "Action ID", required = true)
            @PathVariable Long id) {

        boolean exists = actionService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    /**
     * Count actions with optional filters
     *
     * @param filters search filters (optional)
     * @return number of matching actions
     */
    @GetMapping("/count")
    @Operation(summary = "Count actions", description = "Count actions matching the given filters")
    public ResponseEntity<Long> count(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<Action> spec = filters != null && !filters.isEmpty()
            ? actionService.search(filters)
            : Specification.where(null);

        long count = actionService.count(spec);
        return ResponseEntity.ok(count);
    }

    /**
     * Search actions with dynamic criteria
     *
     * @param searchParams search parameters
     * @param pageable pagination parameters
     * @return page of matching actions
     */
    @PostMapping("/search")
    @Operation(summary = "Search actions", description = "Search actions with dynamic criteria")
    public ResponseEntity<Page<ActionDto>> search(
            @RequestBody Map<String, Object> searchParams,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<Action> spec = actionService.search(searchParams);
        Page<ActionDto> actions = actionService.findAll(spec, pageable);
        return ResponseEntity.ok(actions);
    }
}
