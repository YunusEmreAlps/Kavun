package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
     * Check if a user has permission for a specific page action.
     *
     * @param user the user to check
     * @param pageAction the page action to check
     * @return true if user has permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, PageAction pageAction) {
        if (user == null || pageAction == null) {
            return false;
        }

        return hasPermission(user, pageAction.getId());
    }

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
     * Check if a user has permission for a specific page action ID.
     *
     * @param user the user to check
     * @param pageActionId the page action ID to check
     * @return true if user has permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, Long pageActionId) {
        if (user == null || pageActionId == null) {
            return false;
        }

        return hasPermission(user.getId(), pageActionId);
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
     * Check if a user has permission for multiple page actions.
     *
     * @param user the user to check
     * @param pageActionIds the list of page action IDs to check
     * @return list of page action IDs that the user has permission for
     */
    @Transactional(readOnly = true)
    public List<Long> filterPermittedPageActions(User user, List<Long> pageActionIds) {
        if (user == null || pageActionIds == null || pageActionIds.isEmpty()) {
            return List.of();
        }

        return pageActionIds.stream()
                .filter(pageActionId -> hasPermission(user, pageActionId))
                .collect(Collectors.toList());
    }


}
