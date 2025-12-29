package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the UserRole entity.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("""
                SELECT ur FROM UserRole ur
                JOIN FETCH ur.role r
                WHERE ur.user.id = :userId
                AND ur.deleted = false
                ORDER BY ur.createdAt DESC
            """)
    List<UserRole> findActiveRolesByUserId(@Param("userId") Long userId);

    @Query("""
                SELECT ur FROM UserRole ur
                JOIN FETCH ur.user u
                WHERE ur.role.id = :roleId
                AND ur.deleted = false
            """)
    List<UserRole> findActiveUsersByRoleId(@Param("roleId") Long roleId);

    boolean existsByUserIdAndRoleIdAndDeletedFalse(Long userId, Long roleId);

    Optional<UserRole> findByUserIdAndRoleIdAndDeletedFalse(Long userId, Long roleId);

    @Query("""
                SELECT COUNT(ur) FROM UserRole ur
                WHERE ur.user.id = :userId
                AND ur.deleted = false
            """)
    long countActiveRolesByUserId(@Param("userId") Long userId);
}
