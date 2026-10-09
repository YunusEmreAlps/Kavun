package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.web.payload.response.ActionResponse;
import com.kavun.web.payload.response.NavigationItemResponse;
import com.kavun.web.payload.response.NavigationResponse;
import com.kavun.web.payload.response.PageActionsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for building navigation tree with permissions.
 *
 * <p>Pages, page actions and the user's permissions are each loaded with a single query and the
 * tree is assembled in memory, so the number of queries does not grow with the number of pages or
 * actions.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationService {

  private final PageRepository pageRepository;
  private final PageActionRepository pageActionRepository;
  private final PermissionCheckService permissionCheckService;

  /**
   * Build navigation tree for a user with permission checks.
   *
   * @param userId the ID of the user to build navigation for
   * @return navigation response with hierarchical structure
   */
  @Transactional(readOnly = true)
  public NavigationResponse buildNavigation(Long userId) {
    if (userId == null) {
      return NavigationResponse.builder().navigation(List.of()).build();
    }

    Set<Long> permittedPageActionIds = permissionCheckService.findPermittedPageActionIds(userId);
    if (permittedPageActionIds.isEmpty()) {
      return NavigationResponse.builder().navigation(List.of()).build();
    }

    // Pages are loaded before page actions so that pa.getPage() resolves to the already managed
    // instances whose parent association has been fetched.
    List<WebPage> pages = pageRepository.findAllActiveWithParent();
    Map<Long, List<WebPage>> childrenByParentId = pages.stream()
        .filter(page -> page.getParent() != null)
        .collect(Collectors.groupingBy(page -> page.getParent().getId()));

    Map<Long, List<PageAction>> actionsByPageId = pageActionRepository.findAllActiveWithDetails().stream()
        .collect(Collectors.groupingBy(pa -> pa.getPage().getId()));

    NavigationContext context = new NavigationContext(childrenByParentId, actionsByPageId, permittedPageActionIds);

    List<NavigationItemResponse> navigationItems = pages.stream()
        .filter(page -> page.getParent() == null)
        .map(page -> buildNavigationItem(page, context, 0))
        .filter(Objects::nonNull) // Only include accessible pages
        .toList();

    LOG.debug("Built navigation for user {} with {} root items", userId, navigationItems.size());

    return NavigationResponse.builder()
        .navigation(navigationItems)
        .build();
  }

  /**
   * Build a single navigation item with its children and actions.
   *
   * @param page    the page to build navigation item for
   * @param context the preloaded pages, page actions and permissions
   * @param level   the current level in the hierarchy
   * @return navigation item or null if no access
   */
  private NavigationItemResponse buildNavigationItem(WebPage page, NavigationContext context, int level) {
    // Check if the page should be shown in the menu
    /*
     * if (!page.isShowInMenu()) {
     * return null;
     * }
     */

    List<PageAction> pageActions = context.actionsByPageId().getOrDefault(page.getId(), List.of());

    // No VIEW permission, exclude the page and its subtree from navigation
    if (!hasViewPermission(pageActions, context.permittedPageActionIds())) {
      return null;
    }

    List<NavigationItemResponse> childItems = context.childrenByParentId()
        .getOrDefault(page.getId(), List.of()).stream()
        .map(child -> buildNavigationItem(child, context, level + 1))
        .filter(Objects::nonNull)
        .toList();

    return NavigationItemResponse.builder()
        .id(page.getId())
        .code(page.getCode())
        .label(page.getName())
        .url(page.getUrl())
        .icon(page.getIcon())
        .level(level)
        .access(true)
        .navigable(page.isNavigable())
        .showInMenu(page.isShowInMenu())
        .actions(buildPageActions(pageActions, context.permittedPageActionIds()))
        .children(childItems)
        .build();
  }

  /**
   * Check if the page's VIEW action is among the permitted page actions.
   *
   * @param pageActions            the page actions of the page
   * @param permittedPageActionIds the page actions the user has permission for
   * @return true if user has VIEW permission, false otherwise
   */
  private boolean hasViewPermission(List<PageAction> pageActions, Set<Long> permittedPageActionIds) {
    return pageActions.stream()
        .anyMatch(pa -> pa.isViewAction() && permittedPageActionIds.contains(pa.getId()));
  }

  /**
   * Build action responses for a page, excluding the VIEW action.
   *
   * @param pageActions            the page actions of the page
   * @param permittedPageActionIds the page actions the user has permission for
   * @return list of action responses
   */
  private List<ActionResponse> buildPageActions(List<PageAction> pageActions, Set<Long> permittedPageActionIds) {
    return pageActions.stream()
        .filter(pa -> !pa.isViewAction()) // Exclude VIEW action from the list
        .filter(pa -> permittedPageActionIds.contains(pa.getId())) // Only include actions user has permission for
        .map(pa -> {
          Action action = pa.getAction();
          return ActionResponse.builder()
              .code(action.getCode())
              .label(pa.getLabel())
              .type(action.getType().name())
              .build();
        })
        .toList();
  }

  /**
   * Get actions for a specific page.
   *
   * @param pageId the page ID
   * @param userId the ID of the user to check permissions for
   * @return page actions response
   */
  @Transactional(readOnly = true)
  public PageActionsResponse getPageActions(Long pageId, Long userId) {
    if (pageId == null || userId == null) {
      return PageActionsResponse.builder().actions(List.of()).build();
    }

    List<PageAction> pageActions = pageActionRepository.findByPageIdWithDetails(pageId);
    if (pageActions.isEmpty()) {
      LOG.warn("Page not found or has no actions: {}", pageId);
      return PageActionsResponse.builder().actions(List.of()).build();
    }

    Set<Long> permittedPageActionIds = permissionCheckService.findPermittedPageActionIds(userId);

    // Check if user has VIEW permission for this page
    if (!hasViewPermission(pageActions, permittedPageActionIds)) {
      LOG.warn("User {} does not have VIEW permission for page {}", userId, pageId);
      return PageActionsResponse.builder().actions(List.of()).build();
    }

    List<ActionResponse> actions = buildPageActions(pageActions, permittedPageActionIds);

    LOG.debug("Actions for page {}: {}", pageId, actions);

    return PageActionsResponse.builder()
        .actions(actions)
        .build();
  }

  private record NavigationContext(
      Map<Long, List<WebPage>> childrenByParentId,
      Map<Long, List<PageAction>> actionsByPageId,
      Set<Long> permittedPageActionIds) {
  }
}
