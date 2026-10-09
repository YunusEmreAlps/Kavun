package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.shared.dto.PermissionGrantDto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * The PermissionRepository class exposes implementation from JpaRepository on Permission
 * entity .
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface PermissionRepository extends BaseRepository<Permission> {

    @Query("SELECT p FROM Permission p WHERE p.pageAction.id = :pageActionId " +
           "AND p.entityType = 'USER' AND p.entityId = :userId " +
           "AND p.deleted = false")
    List<Permission> findUserPermissionsByPageAction(Long userId, Long pageActionId);

    @Query("SELECT p FROM Permission p WHERE p.pageAction.id = :pageActionId " +
           "AND p.entityType = 'ROLE' AND p.entityId IN :roleIds " +
           "AND p.deleted = false")
    List<Permission> findRolePermissionsByPageAction(List<Long> roleIds, Long pageActionId);

    @Query("SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId AND ur.deleted = false")
    List<Long> findRoleIdsByUserId(Long userId);

    @Query("SELECT new com.kavun.shared.dto.PermissionGrantDto(p.pageAction.id, p.entityType, p.granted, p.expiresAt) " +
           "FROM Permission p WHERE p.deleted = false AND (" +
           "(p.entityType = 'USER' AND p.entityId = :userId) OR " +
           "(p.entityType = 'ROLE' AND p.entityId IN " +
           "(SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId AND ur.deleted = false)))")
    List<PermissionGrantDto> findPermissionGrantsByUserId(Long userId);

    @Modifying
    @Query("UPDATE Permission p SET p.granted = false WHERE p.expiresAt IS NOT NULL " +
           "AND p.expiresAt < :now AND p.granted = true AND p.deleted = false")
    int expirePermissions(LocalDateTime now);
}
