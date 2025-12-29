package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.Page;
import com.kavun.backend.service.user.PageService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pages")
@RequiredArgsConstructor
@Tag(name = "06. Pages", description = "APIs for managing pages")
public class PageRestApi {
    private final PageService pageService;

    /**
     * Get all pages
     *
     * @return list of pages
     */
    @GetMapping
    public List<Page> getAll() {
        return pageService.findAll();
    }

    /**
     * Get a page by its ID
     *
     * @param id the ID of the page
     * @return the page if found, otherwise 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Page> getById(@PathVariable Long id) {
        return pageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new page
     *
     * @param page the page to create
     * @return the created page
     */
    @PostMapping
    public Page create(@RequestBody Page page) {
        return pageService.save(page);
    }

    /**
     * Delete a page by its ID
     *
     * @param id the ID of the page to delete
     * @return 204 No Content if deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
