package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.user.NavigationService;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.response.NavigationResponse;
import com.kavun.web.payload.response.PageActionsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
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
  @Operation(summary = "Get user navigation", description = "Returns the navigation tree for the authenticated user with permission-based filtering")
  public ResponseEntity<NavigationResponse> getNavigation() {
    UserDto userDto = SecurityUtils.getAuthorizedUserDto();
    User user = userRepository.findById(userDto.getId())
        .orElseThrow(() -> new EntityNotFoundException(UserConstants.USER_NOT_FOUND));

    NavigationResponse navigation = navigationService.buildNavigation(user);
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
    User user = userRepository.findById(userDto.getId())
        .orElseThrow(() -> new EntityNotFoundException(UserConstants.USER_NOT_FOUND));

    PageActionsResponse actions = navigationService.getPageActions(pageId, user);
    return ResponseEntity.ok(actions);
  }
}
