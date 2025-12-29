package com.kavun.web.rest.v1;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.service.user.ActionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/actions")
@RequiredArgsConstructor
@Tag(name = "05. Actions", description = "APIs for managing actions")
public class ActionRestApi {
    private final ActionService actionService;

    /**
     * Get all actions
     *
     * @return list of actions
     */
    @GetMapping
    public List<Action> getAll() {
        return actionService.findAll();
    }

    /**
     * Get action by ID
     *
     * @param id action ID
     * @return action entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Action> getById(@PathVariable Long id) {
        return actionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new action
     *
     * @param action action entity
     * @return created action
     */
    @PostMapping
    public Action create(@RequestBody Action action) {
        return actionService.save(action);
    }

    /**
     * Delete action by ID
     *
     * @param id action ID
     * @return response entity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        actionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
