package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.repository.PermissionRepository;
import com.kavun.enums.EntityType;
import com.kavun.shared.dto.PermissionGrantDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for checking permissions based on the priority system.
 *
 * Permission Check Priority:
 * 1. USER level with granted=false and valid expires_at → DENY (highest priority)
 * 2. USER level with granted=true and valid expires_at → ALLOW
 * 3. ROLE level with any granted=false and valid expires_at → DENY
 * 4. ROLE level with at least one granted=true and valid expires_at → ALLOW
 * 5. No permission record → DENY (default deny)
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCheckService {

    private final PermissionRepository permissionRepository;

    /**
     * Check if a user has permission for a specific page action by user ID.
     * This method loads the user within the transaction to avoid LazyInitializationException.
     *
     * @param userId the user ID to check
     * @param pageAction the page action to check
     * @return true if user has permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPermissionByUserId(Long userId, PageAction pageAction) {
        if (userId == null || pageAction == null) {
            return false;
        }

        return hasPermission(userId, pageAction.getId());
    }

    /**
     * Check if a user has permission for a specific page action ID by user ID.
     * This method works with IDs only to avoid LazyInitializationException.
     *
     * @param userId the user ID to check
     * @param pageActionId the page action ID to check
     * @return true if user has permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, Long pageActionId) {
        if (userId == null || pageActionId == null) {
            return false;
        }

        // Step 1: Check USER level permissions
        List<Permission> userPermissions = permissionRepository
                .findUserPermissionsByPageAction(userId, pageActionId);

        // Filter valid permissions (not expired)
        List<Permission> validUserPermissions = userPermissions.stream()
                .filter(Permission::isValid)
                .collect(Collectors.toList());

        // Check for explicit USER deny (highest priority)
        boolean hasUserDeny = userPermissions.stream()
                .anyMatch(p -> !p.isGranted() && !p.isExpired());

        if (hasUserDeny) {
            LOG.debug("User {} explicitly denied access to page action {}", userId, pageActionId);
            return false;
        }

        // Check for USER allow
        boolean hasUserAllow = validUserPermissions.stream()
                .anyMatch(Permission::isGranted);

        if (hasUserAllow) {
            LOG.debug("User {} explicitly allowed access to page action {}", userId, pageActionId);
            return true;
        }

        // Step 2: Check ROLE level permissions
        // Get role IDs directly from repository to avoid LazyInitializationException
        List<Long> roleIds = permissionRepository.findRoleIdsByUserId(userId);

        if (roleIds.isEmpty()) {
            LOG.debug("User {} has no roles, access denied to page action {}", userId, pageActionId);
            return false;
        }

        List<Permission> rolePermissions = permissionRepository
                .findRolePermissionsByPageAction(roleIds, pageActionId);

        // Filter valid permissions (not expired)
        List<Permission> validRolePermissions = rolePermissions.stream()
                .filter(Permission::isValid)
                .collect(Collectors.toList());

        // Check for any ROLE deny
        boolean hasRoleDeny = rolePermissions.stream()
                .anyMatch(p -> !p.isGranted() && !p.isExpired());

        if (hasRoleDeny) {
            LOG.debug("User {} denied access via role to page action {}", userId, pageActionId);
            return false;
        }

        // Check for any ROLE allow
        boolean hasRoleAllow = validRolePermissions.stream()
                .anyMatch(Permission::isGranted);

        if (hasRoleAllow) {
            LOG.debug("User {} allowed access via role to page action {}", userId, pageActionId);
            return true;
        }

        // Step 3: Default deny
        LOG.debug("User {} has no valid permission for page action {}, access denied", userId, pageActionId);
        return false;
    }

    /**
     * Scheduled task to expire permissions.
     * Runs every hour to update expired permissions.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void expirePermissions() {
        int expiredCount = permissionRepository.expirePermissions(LocalDateTime.now());
        if (expiredCount > 0) {
            LOG.info("Expired {} permissions", expiredCount);
        }
    }

    /**
     * Resolve every page action the user is allowed to access, using a single query.
     * Applies the same priority rules as {@link #hasPermission(Long, Long)}: expired records are
     * ignored, a USER level decision overrides any ROLE level one, and within a level any deny wins.
     *
     * @param userId the user ID
     * @return IDs of the page actions the user has permission for
     */
    @Transactional(readOnly = true)
    public Set<Long> findPermittedPageActionIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }

        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<PermissionGrantDto>> userGrants = new HashMap<>();
        Map<Long, List<PermissionGrantDto>> roleGrants = new HashMap<>();

        for (PermissionGrantDto grant : permissionRepository.findPermissionGrantsByUserId(userId)) {
            if (grant.isExpired(now)) {
                continue;
            }
            var grants = grant.entityType() == EntityType.USER ? userGrants : roleGrants;
            grants.computeIfAbsent(grant.pageActionId(), id -> new ArrayList<>()).add(grant);
        }

        Set<Long> candidates = new HashSet<>(userGrants.keySet());
        candidates.addAll(roleGrants.keySet());

        Set<Long> permitted = new HashSet<>();
        for (Long pageActionId : candidates) {
            boolean allowed = decide(userGrants.get(pageActionId))
                    .or(() -> decide(roleGrants.get(pageActionId)))
                    .orElse(false);
            if (allowed) {
                permitted.add(pageActionId);
            }
        }

        LOG.debug("User {} has {} permitted page actions", userId, permitted.size());
        return permitted;
    }

    /**
     * Check whether a user holds a permission identified by its page and action codes, using a
     * single query. Applies the same priority rules as {@link #hasPermission(Long, Long)}.
     *
     * @param userId     the user ID
     * @param pageCode   the page code
     * @param actionCode the action code
     * @return true if the user has the permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String pageCode, String actionCode) {
        if (userId == null || pageCode == null || actionCode == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        List<PermissionGrantDto> userGrants = new ArrayList<>();
        List<PermissionGrantDto> roleGrants = new ArrayList<>();
        for (PermissionGrantDto grant
                : permissionRepository.findPermissionGrantsByUserIdAndPageAction(userId, pageCode, actionCode)) {
            if (!grant.isExpired(now)) {
                (grant.entityType() == EntityType.USER ? userGrants : roleGrants).add(grant);
            }
        }

        return decide(userGrants).or(() -> decide(roleGrants)).orElse(false);
    }

    /**
     * Decide a single permission level from its non-expired grants.
     *
     * @param grants the non-expired grants of one level for one page action
     * @return empty if the level has no grants, false if any grant denies, true otherwise
     */
    private static Optional<Boolean> decide(List<PermissionGrantDto> grants) {
        if (grants == null || grants.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(grants.stream().allMatch(PermissionGrantDto::granted));
    }


}
