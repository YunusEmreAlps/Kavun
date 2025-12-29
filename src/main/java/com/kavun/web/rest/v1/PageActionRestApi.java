package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.service.user.PageActionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/page-actions")
@RequiredArgsConstructor
@Tag(name = "07. Page Actions", description = "APIs for managing page actions")
public class PageActionRestApi {
    private final PageActionService pageActionService;

    /**
     * Get all page actions
     *
     * @return list of page actions
     */
    @GetMapping
    public List<PageAction> getAll() {
        return pageActionService.findAll();
    }

    /**
     * Get a page action by its ID
     *
     * @param id the ID of the page action
     * @return the page action if found, otherwise 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<PageAction> getById(@PathVariable Long id) {
        return pageActionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new page action
     *
     * @param pageAction the page action to create
     * @return the created page action
     */
    @PostMapping
    public PageAction create(@RequestBody PageAction pageAction) {
        return pageActionService.save(pageAction);
    }

    /**
     * Delete a page action by its ID
     *
     * @param id the ID of the page action to delete
     * @return 204 No Content if deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pageActionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
