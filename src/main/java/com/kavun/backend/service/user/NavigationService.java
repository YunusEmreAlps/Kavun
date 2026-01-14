package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.User;
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
import java.util.stream.Collectors;

/**
 * Service for building navigation tree with permissions.
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
     * @param user the user to build navigation for
     * @return navigation response with hierarchical structure
     */
    @Transactional(readOnly = true)
    public NavigationResponse buildNavigation(User user) {
        if (user == null) {
            return NavigationResponse.builder().navigation(List.of()).build();
        }

        // Get all root level pages
        List<WebPage> rootPages = pageRepository.findByParentIsNullAndDeletedFalseOrderByDisplayOrder();

        // Build navigation items recursively
        List<NavigationItemResponse> navigationItems = rootPages.stream()
                .map(page -> buildNavigationItem(page, user, 0))
                .filter(item -> item != null && item.isAccess()) // Only include accessible pages
                .collect(Collectors.toList());

        return NavigationResponse.builder()
                .navigation(navigationItems)
                .build();
    }

    /**
     * Build a single navigation item with its children and actions.
     *
     * @param page the page to build navigation item for
     * @param user the user to check permissions for
     * @param level the current level in the hierarchy
     * @return navigation item or null if no access
     */
    private NavigationItemResponse buildNavigationItem(WebPage page, User user, int level) {
        // Check if user has VIEW permission for this page
        boolean hasAccess = hasViewPermission(page, user);

        if (!hasAccess) {
            return null; // No VIEW permission, exclude from navigation
        }

        // Build navigation item
        NavigationItemResponse.NavigationItemResponseBuilder builder = NavigationItemResponse.builder()
                .id(page.getId())
                .code(page.getCode())
                .label(page.getName())
                .url(page.getUrl())
                .icon(page.getIcon())
                .level(level)
                .access(true);

        // Add actions for this page
        List<ActionResponse> actions = buildPageActions(page, user);
        builder.actions(actions);

        // Recursively build children
        List<WebPage> children = pageRepository.findByParentIdAndDeletedFalseOrderByDisplayOrder(page.getId());
        List<NavigationItemResponse> childItems = children.stream()
                .map(child -> buildNavigationItem(child, user, level + 1))
                .filter(item -> item != null && item.isAccess())
                .collect(Collectors.toList());

        builder.children(childItems);

        return builder.build();
    }

    /**
     * Check if user has VIEW permission for a page.
     *
     * @param page the page to check
     * @param user the user to check
     * @return true if user has VIEW permission, false otherwise
     */
    private boolean hasViewPermission(WebPage page, User user) {
        // Find VIEW page action for this page
        List<PageAction> pageActions = pageActionRepository.findByPageIdAndDeletedFalse(page.getId());

        LOG.debug("Found {} page actions for page {}", pageActions.size(), page.getCode());

        PageAction viewAction = pageActions.stream()
                .filter(PageAction::isViewAction)
                .findFirst()
                .orElse(null);

        // If no VIEW action exists, deny access
        if (viewAction == null) {
            LOG.debug("No VIEW action found for page {}", page.getCode());
            return false;
        }

        // Check permission
        boolean hasPermission = permissionCheckService.hasPermission(user, viewAction);
        LOG.debug("User {} has VIEW permission for page {}: {}", user.getId(), page.getCode(), hasPermission);
        return hasPermission;
    }

    /**
     * Build action responses for a page.
     *
     * @param page the page to build actions for
     * @param user the user to check permissions for
     * @return list of action responses
     */
    private List<ActionResponse> buildPageActions(WebPage page, User user) {
        List<PageAction> pageActions = pageActionRepository.findByPageIdWithDetails(page.getId());

        LOG.debug("Building actions for page {}. Found {} page actions (with details)",
                page.getCode(), pageActions.size());

        List<ActionResponse> actions = pageActions.stream()
                .filter(pa -> !pa.isViewAction()) // Exclude VIEW action from the list
                .map(pa -> {
                    boolean hasAccess = permissionCheckService.hasPermission(user, pa);
                    Action action = pa.getAction();

                    LOG.debug("Action {} for page {}: hasAccess={}",
                            action.getCode(), page.getCode(), hasAccess);

                    return ActionResponse.builder()
                            .code(action.getCode())
                            .label(pa.getLabel())
                            .type(action.getType().name())
                            .build();
                })
                .toList();

        LOG.debug("Returning {} actions for page {}", actions.size(), page.getCode());
        return actions;
    }

    /**
     * Get actions for a specific page.
     *
     * @param pageId the page ID
     * @param user the user to check permissions for
     * @return page actions response
     */
    @Transactional(readOnly = true)
    public PageActionsResponse getPageActions(Long pageId, User user) {
        if (pageId == null || user == null) {
            return PageActionsResponse.builder().actions(List.of()).build();
        }

        WebPage page = pageRepository.findById(pageId).orElse(null);
        if (page == null) {
            LOG.warn("Page not found: {}", pageId);
            return PageActionsResponse.builder().actions(List.of()).build();
        }

        // Check if user has VIEW permission for this page
        if (!hasViewPermission(page, user)) {
            LOG.warn("User {} does not have VIEW permission for page {}", user.getId(), pageId);
            return PageActionsResponse.builder().actions(List.of()).build();
        }

        List<ActionResponse> actions = buildPageActions(page, user);

        LOG.debug("Actions for page {}: {}", page.getCode(), actions.toString());

        return PageActionsResponse.builder()
                .actions(actions)
                .build();
    }
}
