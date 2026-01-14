package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.user.NavigationService;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.response.NavigationResponse;
import com.kavun.web.payload.response.PageActionsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * This class handles all rest calls for navigation and permissions.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/navigation")
@Tag(name = "08. Navigation API", description = "This API provides endpoints for navigation and page permissions.")
public class NavigationRestApi {

    private final NavigationService navigationService;
    private final UserRepository userRepository;

    /**
     * Get navigation tree for the current authenticated user.
     * Returns hierarchical navigation structure with permission checks.
     *
     * @return navigation response with accessible pages and actions
     */
    @Loggable
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get user navigation",
        description = "Returns the navigation tree for the authenticated user with permission-based filtering"
    )
    public ResponseEntity<NavigationResponse> getNavigation() {
        try {
            // Get current user
            UserDto userDto = SecurityUtils.getAuthorizedUserDto();
            if (userDto == null) {
                LOG.warn("Unauthorized access attempt to navigation");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Get user entity
            User user = userRepository.findById(userDto.getId()).orElse(null);
            if (user == null) {
                LOG.warn("User not found: {}", userDto.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Build navigation
            NavigationResponse navigation = navigationService.buildNavigation(user);
            return ResponseEntity.ok(navigation);

        } catch (Exception e) {
            LOG.error("Error building navigation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get actions for a specific page.
     * Returns all actions available for the page with permission checks.
     *
     * @param pageId the page ID
     * @return page actions response with accessible actions
     */
    @Loggable
    @GetMapping("/page/{pageId}/actions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get page actions",
        description = "Returns all actions available for a specific page with permission-based filtering"
    )
    public ResponseEntity<PageActionsResponse> getPageActions(
            @Parameter(description = "Page ID", required = true)
            @PathVariable Long pageId) {
        try {
            // Get current user
            UserDto userDto = SecurityUtils.getAuthorizedUserDto();
            if (userDto == null) {
                LOG.warn("Unauthorized access attempt to page actions");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Get user entity
            User user = userRepository.findById(userDto.getId()).orElse(null);
            if (user == null) {
                LOG.warn("User not found: {}", userDto.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Get page actions
            PageActionsResponse actions = navigationService.getPageActions(pageId, user);
            return ResponseEntity.ok(actions);

        } catch (Exception e) {
            LOG.error("Error getting page actions for page: {}", pageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
