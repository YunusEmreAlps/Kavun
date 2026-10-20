package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.backend.persistent.repository.PermissionRepository;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.security.permission.AccessControlService;
import com.kavun.backend.service.security.permission.PermissionCatalog;
import com.kavun.enums.EntityType;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.dto.mapper.PermissionMapper;
import com.kavun.shared.request.PermissionRequest;
import com.kavun.shared.util.core.TenantContext;
import com.kavun.web.payload.response.RolePermissionMatrixResponse;
import com.kavun.web.payload.response.RolePermissionMatrixResponse.ActionColumn;
import com.kavun.web.payload.response.RolePermissionMatrixResponse.Cell;
import com.kavun.web.payload.response.RolePermissionMatrixResponse.PageRow;
import com.kavun.web.payload.response.RolePermissionMatrixResponse.RoleSummary;
import com.kavun.web.payload.response.RolePermissionMatrixResponse.State;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* Permission service to provide implementation for the definitions about a permission, and the
* permission matrix of a role.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
@Slf4j
@Service
@Transactional
public class PermissionService
        extends AbstractService<PermissionRequest, Permission, PermissionDto, PermissionRepository, PermissionMapper> {

    private final PermissionCatalog permissionCatalog;
    private final AccessControlService accessControlService;
    private final RoleRepository roleRepository;
    private final PageRepository pageRepository;
    private final PageActionRepository pageActionRepository;

    public PermissionService(PermissionMapper mapper, PermissionRepository repository,
            PermissionCatalog permissionCatalog, AccessControlService accessControlService,
            RoleRepository roleRepository, PageRepository pageRepository,
            PageActionRepository pageActionRepository) {
        super(mapper, repository);
        this.permissionCatalog = permissionCatalog;
        this.accessControlService = accessControlService;
        this.roleRepository = roleRepository;
        this.pageRepository = pageRepository;
        this.pageActionRepository = pageActionRepository;
    }

    @Override
    protected void beforeCreate(Permission permission) {
        checkGrantable(permission);
    }

    @Override
    protected void beforeUpdate(Permission permission) {
        checkGrantable(permission);
    }

    // Platform-only pages cannot be granted inside other tenants, and nobody grants what it lacks.
    private void checkGrantable(Permission permission) {
        String page = permission.getPageAction().getPage().getCode();
        if (permissionCatalog.isPlatformOnly(page) && !TenantContext.isPlatform()) {
            throw new AccessDeniedException("Page " + page + " is reserved for the platform");
        }
        if (permission.isGranted()) {
            accessControlService.checkCanGrant(List.of(permission.getPageAction().getId()));
        }
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of("entityType", "entityId", "pageAction", "granted", "expiresAt", "createdAt");
    }

    /**
     * The permission matrix of a role: the pages the current tenant may use, in menu order, with a
     * cell per page action.
     *
     * @param roleId the role
     * @return the matrix
     */
    @Transactional(readOnly = true)
    public RolePermissionMatrixResponse getRoleMatrix(Long roleId) {
        Role role = findRole(roleId);
        List<PageAction> pageActions = matrixPageActions();
        Map<Long, Permission> grants = grantsOf(role);
        Predicate<Long> grantable = accessControlService.grantablePageActions();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, List<Cell>> cellsByPage = pageActions.stream().collect(Collectors.groupingBy(
                pageAction -> pageAction.getPage().getId(),
                Collectors.mapping(pageAction -> cell(pageAction, grants.get(pageAction.getId()), grantable, now),
                        Collectors.toList())));

        List<ActionColumn> actions = pageActions.stream()
                .map(PageAction::getAction)
                .distinct()
                .sorted(Comparator.comparing((Action action) -> Objects.requireNonNullElse(action.getOrderIndex(), 0))
                        .thenComparing(Action::getId))
                .map(action -> new ActionColumn(action.getCode(), action.getName()))
                .toList();

        return new RolePermissionMatrixResponse(
                new RoleSummary(role.getId(), role.getName(), role.getLabel()),
                accessControlService.canEditPermissionsOf(role),
                actions,
                pageRows(cellsByPage));
    }

    /**
     * Grants a role exactly the checked cells of its matrix. Only cells whose state changes are
     * written: a newly checked cell is granted (reusing the role's existing record of the page
     * action, even a soft-deleted or denying one), an unchecked granted cell is soft deleted, and
     * denials stay unless their cell is checked.
     *
     * @param roleId the role
     * @param pageActionIds the checked cells
     * @return the updated matrix
     * @throws AccessDeniedException if the caller may not change the role's grants, or grants a page
     *     action it does not hold itself
     */
    public RolePermissionMatrixResponse updateRoleMatrix(Long roleId, Set<Long> pageActionIds) {
        Role role = findRole(roleId);
        accessControlService.checkCanEditPermissionsOf(role);

        Map<Long, PageAction> cells = matrixPageActions().stream()
                .collect(Collectors.toMap(PageAction::getId, Function.identity()));
        List<Long> unknown = pageActionIds.stream().filter(id -> !cells.containsKey(id)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Page actions not in the permission matrix: " + unknown);
        }

        Map<Long, Permission> grants = grantsOf(role);
        LocalDateTime now = LocalDateTime.now();
        List<Long> granting = pageActionIds.stream()
                .filter(id -> state(grants.get(id), now) != State.GRANTED)
                .toList();
        accessControlService.checkCanGrant(granting);

        granting.forEach(id -> grant(role, cells.get(id), grants.get(id)));
        grants.forEach((id, permission) -> {
            if (cells.containsKey(id) && !pageActionIds.contains(id) && state(permission, now) == State.GRANTED) {
                applySoftDelete(permission);
                repository.save(permission);
            }
        });
        LOG.info("Permission matrix of role {} saved, {} page actions granted", role.getName(), pageActionIds.size());
        return getRoleMatrix(roleId);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> !role.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + roleId));
    }

    // The active page actions of the pages the current tenant may use.
    private List<PageAction> matrixPageActions() {
        return pageActionRepository.findAllActiveWithDetails().stream()
                .filter(pageAction -> !pageAction.getAction().isDeleted())
                .filter(pageAction -> isVisible(pageAction.getPage()))
                .toList();
    }

    private boolean isVisible(WebPage page) {
        return TenantContext.isPlatform() || !permissionCatalog.isPlatformOnly(page.getCode());
    }

    // The role's records by page action, soft-deleted ones included.
    private Map<Long, Permission> grantsOf(Role role) {
        return repository.findAllByEntityTypeAndEntityId(EntityType.ROLE, role.getId()).stream()
                .collect(Collectors.toMap(permission -> permission.getPageAction().getId(), Function.identity()));
    }

    // Expired records count as absent, like in the permission checks.
    private static State state(Permission permission, LocalDateTime now) {
        if (permission == null || permission.isDeleted()
                || (permission.getExpiresAt() != null && permission.getExpiresAt().isBefore(now))) {
            return State.NONE;
        }
        return permission.isGranted() ? State.GRANTED : State.DENIED;
    }

    private static Cell cell(PageAction pageAction, Permission permission, Predicate<Long> grantable,
            LocalDateTime now) {
        State state = state(permission, now);
        return new Cell(
                pageAction.getAction().getCode(),
                pageAction.getId(),
                state,
                state == State.NONE ? null : permission.getExpiresAt(),
                grantable.test(pageAction.getId()));
    }

    private void grant(Role role, PageAction pageAction, Permission existing) {
        Permission permission = existing;
        if (permission == null) {
            permission = new Permission();
            permission.setTenantCode(role.getTenantCode());
            permission.setEntityType(EntityType.ROLE);
            permission.setEntityId(role.getId());
            permission.setPageAction(pageAction);
        } else if (permission.isDeleted()) {
            applyRestore(permission);
        }
        permission.setGranted(true);
        permission.setExpiresAt(null);
        repository.save(permission);
    }

    // Pages in menu order, depth first; a page is listed if it or one of its descendants has cells.
    private List<PageRow> pageRows(Map<Long, List<Cell>> cellsByPage) {
        List<WebPage> pages = pageRepository.findAllActiveWithParent().stream().filter(this::isVisible).toList();
        Set<Long> visibleIds = pages.stream().map(WebPage::getId).collect(Collectors.toSet());
        Map<Long, List<WebPage>> children = pages.stream()
                .filter(page -> page.getParent() != null && visibleIds.contains(page.getParent().getId()))
                .collect(Collectors.groupingBy(page -> page.getParent().getId()));

        List<PageRow> rows = new ArrayList<>();
        pages.stream()
                .filter(page -> page.getParent() == null || !visibleIds.contains(page.getParent().getId()))
                .forEach(root -> rows.addAll(pageRows(root, 0, children, cellsByPage)));
        return rows;
    }

    private List<PageRow> pageRows(WebPage page, int level, Map<Long, List<WebPage>> children,
            Map<Long, List<Cell>> cellsByPage) {
        List<PageRow> descendants = children.getOrDefault(page.getId(), List.of()).stream()
                .flatMap(child -> pageRows(child, level + 1, children, cellsByPage).stream())
                .toList();
        List<Cell> cells = cellsByPage.getOrDefault(page.getId(), List.of());
        if (cells.isEmpty() && descendants.isEmpty()) {
            return List.of();
        }
        List<PageRow> rows = new ArrayList<>();
        rows.add(new PageRow(page.getId(), page.getCode(), page.getName(), level, cells));
        rows.addAll(descendants);
        return rows;
    }
}
