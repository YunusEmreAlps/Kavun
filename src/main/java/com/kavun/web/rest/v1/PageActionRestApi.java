package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.service.user.PageActionService;
import com.kavun.shared.dto.PageActionDto;
import com.kavun.shared.request.PageActionRequest;

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
@RequestMapping("/api/v1/page-actions")
@RequiredArgsConstructor
@Tag(name = "06. Page Action Management", description = "APIs for managing page actions")
public class PageActionRestApi {

    private final PageActionService pageActionService;

    /**
     * Get all pages with pagination and filtering
     *
     * @param filters search filters (optional)
     * @param pageable pagination parameters
     * @return page of page DTOs
     */
    @GetMapping
    @Operation(summary = "Get all page actions", description = "Retrieve all page actions with pagination and optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved page actions")
    })
    public ResponseEntity<Page<PageActionDto>> getAll(
            @RequestParam(required = false) Map<String, Object> filters,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<PageAction> spec = filters != null && !filters.isEmpty()
            ? pageActionService.search(filters)
            : Specification.where(null);

        Page<PageActionDto> pages = pageActionService.findAll(spec, pageable);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get all pages as list (without pagination)
     *
     * @param filters search filters (optional)
     * @return list of page DTOs
     */
    @GetMapping("/list")
    @Operation(summary = "Get all page actions as list", description = "Retrieve all page actions without pagination")
    public ResponseEntity<List<PageActionDto>> getAllList(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<PageAction> spec = filters != null && !filters.isEmpty()
            ? pageActionService.search(filters)
            : Specification.where(null);

        List<PageActionDto> pages = pageActionService.findAll(spec);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get page by ID
     *
     * @param id page ID
     * @return page DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get page action by ID", description = "Retrieve a specific page action by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page action found"),
        @ApiResponse(responseCode = "404", description = "Page action not found")
    })
    public ResponseEntity<PageActionDto> getById(
            @Parameter(description = "Page action ID", required = true)
            @PathVariable Long id) {

        PageActionDto pageAction = pageActionService.findById(id);
        return ResponseEntity.ok(pageAction);
    }

    /**
     * Create a new page action
     *
     * @param request page creation request
     * @return created page DTO
     */
    @PostMapping
    @Operation(summary = "Create page", description = "Create a new page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Page action created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PageActionDto> create(
            @Valid @RequestBody PageActionRequest request) {

        PageActionDto created = pageActionService.create(request);
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
        @ApiResponse(responseCode = "200", description = "Page action updated successfully"),
        @ApiResponse(responseCode = "404", description = "Page action not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PageActionDto> update(
            @Parameter(description = "Page action ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody PageActionRequest request) {

        PageActionDto updated = pageActionService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete page by ID
     *
     * @param id page ID
     * @return response entity with no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete page action", description = "Soft delete a page action (marks as deleted)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Page action deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Page action not found"),
        @ApiResponse(responseCode = "400", description = "Page action already deleted")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Page action ID", required = true)
            @PathVariable Long id) {

        pageActionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted page
     *
     * @param id page ID
     * @return restored page DTO
     */
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore page action", description = "Restore a soft-deleted page action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page action restored successfully"),
        @ApiResponse(responseCode = "404", description = "Page action not found"),
        @ApiResponse(responseCode = "400", description = "Page action is not deleted")
    })
    public ResponseEntity<PageActionDto> restore(
            @Parameter(description = "Page action ID", required = true)
            @PathVariable Long id) {

        PageActionDto restored = pageActionService.restore(id);
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

        boolean exists = pageActionService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    /**
     * Count pages with optional filters
     *
     * @param filters search filters (optional)
     * @return number of matching page actions
     */
    @GetMapping("/count")
    @Operation(summary = "Count page actions", description = "Count page actions matching the given filters")
    public ResponseEntity<Long> count(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<PageAction> spec = filters != null && !filters.isEmpty()
            ? pageActionService.search(filters)
            : Specification.where(null);

        long count = pageActionService.count(spec);
        return ResponseEntity.ok(count);
    }

    /**
     * Search page actions with dynamic criteria
     *
     * @param searchParams search parameters
     * @param pageable pagination parameters
     * @return page of matching page actions
     */
    @PostMapping("/search")
    @Operation(summary = "Search page actions", description = "Search page actions with dynamic criteria")
    public ResponseEntity<Page<PageActionDto>> search(
            @RequestBody Map<String, Object> searchParams,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<PageAction> spec = pageActionService.search(searchParams);
        Page<PageActionDto> pageActions = pageActionService.findAll(spec, pageable);
        return ResponseEntity.ok(pageActions);
    }
}
