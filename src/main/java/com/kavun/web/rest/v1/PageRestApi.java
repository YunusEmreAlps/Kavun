package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.service.user.PageService;
import com.kavun.shared.dto.PageDto;
import com.kavun.shared.request.PageRequest;

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
@RequestMapping("/api/v1/page")
@RequiredArgsConstructor
@Tag(name = "05. Page Management", description = "APIs for managing pages")
public class PageRestApi {

    private final PageService pageService;

    /**
     * Get all pages with pagination and filtering
     *
     * @param filters search filters (optional)
     * @param pageable pagination parameters
     * @return page of page DTOs
     */
    @GetMapping
    @Operation(summary = "Get all pages", description = "Retrieve all pages with pagination and optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pages")
    })
    public ResponseEntity<Page<PageDto>> getAll(
            @RequestParam(required = false) Map<String, Object> filters,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<WebPage> spec = filters != null && !filters.isEmpty()
            ? pageService.search(filters)
            : Specification.where(null);

        Page<PageDto> pages = pageService.findAll(spec, pageable);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get all pages as list (without pagination)
     *
     * @param filters search filters (optional)
     * @return list of page DTOs
     */
    @GetMapping("/list")
    @Operation(summary = "Get all pages as list", description = "Retrieve all pages without pagination")
    public ResponseEntity<List<PageDto>> getAllList(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<WebPage> spec = filters != null && !filters.isEmpty()
            ? pageService.search(filters)
            : Specification.where(null);

        List<PageDto> pages = pageService.findAll(spec);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get page by ID
     *
     * @param id page ID
     * @return page DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get page by ID", description = "Retrieve a specific page by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page found"),
        @ApiResponse(responseCode = "404", description = "Page not found")
    })
    public ResponseEntity<PageDto> getById(
            @Parameter(description = "Page ID", required = true)
            @PathVariable Long id) {

        PageDto page = pageService.findById(id);
        return ResponseEntity.ok(page);
    }

    /**
     * Create a new page
     *
     * @param request page creation request
     * @return created page DTO
     */
    @PostMapping
    @Operation(summary = "Create page", description = "Create a new page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Page created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PageDto> create(
            @Valid @RequestBody PageRequest request) {

        PageDto created = pageService.create(request);
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
    @Operation(summary = "Update page", description = "Update an existing page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page updated successfully"),
        @ApiResponse(responseCode = "404", description = "Page not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PageDto> update(
            @Parameter(description = "Page ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody PageRequest request) {

        PageDto updated = pageService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete page by ID
     *
     * @param id page ID
     * @return response entity with no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete page", description = "Soft delete a page (marks as deleted)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Page deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Page not found"),
        @ApiResponse(responseCode = "400", description = "Page already deleted")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Page ID", required = true)
            @PathVariable Long id) {

        pageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted page
     *
     * @param id page ID
     * @return restored page DTO
     */
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore page", description = "Restore a soft-deleted page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page restored successfully"),
        @ApiResponse(responseCode = "404", description = "Page not found"),
        @ApiResponse(responseCode = "400", description = "Page is not deleted")
    })
    public ResponseEntity<PageDto> restore(
            @Parameter(description = "Page ID", required = true)
            @PathVariable Long id) {

        PageDto restored = pageService.restore(id);
        return ResponseEntity.ok(restored);
    }

    /**
     * Check if page exists by ID
     *
     * @param id page ID
     * @return true if exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if page exists", description = "Check if a page exists by ID")
    public ResponseEntity<Boolean> existsById(
            @Parameter(description = "Page ID", required = true)
            @PathVariable Long id) {

        boolean exists = pageService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    /**
     * Count pages with optional filters
     *
     * @param filters search filters (optional)
     * @return number of matching pages
     */
    @GetMapping("/count")
    @Operation(summary = "Count pages", description = "Count pages matching the given filters")
    public ResponseEntity<Long> count(
            @RequestParam(required = false) Map<String, Object> filters) {

        Specification<WebPage> spec = filters != null && !filters.isEmpty()
            ? pageService.search(filters)
            : Specification.where(null);

        long count = pageService.count(spec);
        return ResponseEntity.ok(count);
    }

    /**
     * Search pages with dynamic criteria
     *
     * @param searchParams search parameters
     * @param pageable pagination parameters
     * @return page of matching pages
     */
    @PostMapping("/search")
    @Operation(summary = "Search pages", description = "Search pages with dynamic criteria")
    public ResponseEntity<Page<PageDto>> search(
            @RequestBody Map<String, Object> searchParams,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Specification<WebPage> spec = pageService.search(searchParams);
        Page<PageDto> pages = pageService.findAll(spec, pageable);
        return ResponseEntity.ok(pages);
    }
}
