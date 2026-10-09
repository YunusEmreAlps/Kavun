package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.user.NavigationService;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.response.NavigationResponse;
import com.kavun.web.payload.response.PageActionsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * This class handles all rest calls for navigation and permissions.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/navigation")
@Tag(name = "08. Navigation API", description = "This API provides endpoints for navigation and page permissions.")
public class NavigationRestApi {

  private final NavigationService navigationService;

  /**
   * Get navigation tree for the current authenticated user.
   * Returns hierarchical navigation structure with permission checks.
   *
   * @return navigation response with accessible pages and actions
   */
  @Loggable(ignoreResponseData = true)
  @GetMapping
  @Operation(summary = "Get user navigation", description = "Returns the navigation tree for the authenticated user with permission-based filtering")
  public ResponseEntity<NavigationResponse> getNavigation() {
    UserDto userDto = SecurityUtils.getAuthorizedUserDto();

    NavigationResponse navigation = navigationService.buildNavigation(userDto.getId());
    return ResponseEntity.ok(navigation);
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
  @Operation(summary = "Get page actions", description = "Returns all actions available for a specific page with permission-based filtering")
  public ResponseEntity<PageActionsResponse> getPageActions(
      @Parameter(description = "Page ID", required = true) @PathVariable Long pageId) {
    UserDto userDto = SecurityUtils.getAuthorizedUserDto();

    PageActionsResponse actions = navigationService.getPageActions(pageId, userDto.getId());
    return ResponseEntity.ok(actions);
  }
}
