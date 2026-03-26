package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.UserSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for UserSession entity.
 * Manages user session persistence and queries.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 2.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface UserSessionRepository extends BaseRepository<UserSession> {

    /**
     * Find all active sessions for a specific user.
     *
     * @param userId User ID
     * @return List of active sessions
     */
    List<UserSession> findByUserIdAndIsActiveTrueOrderByLoginAtDesc(Long userId);

    /**
     * Find active session by user ID and device ID.
     *
     * @param userId User ID
     * @param deviceId Device ID
     * @return Optional UserSession
     */
    Optional<UserSession> findByUserIdAndDeviceIdAndIsActiveTrue(Long userId, String deviceId);

    /**
     * Find all expired sessions based on timeout duration.
     *
     * @param cutoffTime Sessions with lastActivityAt before this time are considered expired
     * @return List of expired sessions
     */
    @Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.lastActivityAt < :cutoffTime")
    List<UserSession> findExpiredSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count active sessions for a user.
     *
     * @param userId User ID
     * @return Number of active sessions
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find session by refresh token hash.
     *
     * @param refreshTokenHash Hashed refresh token
     * @return Optional UserSession
     */
    Optional<UserSession> findByRefreshTokenHashAndIsActiveTrue(String refreshTokenHash);

    /**
     * Deactivate all sessions for a user (force logout from all devices).
     *
     * @param userId User ID
     * @return Number of sessions updated
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.logoutAt = :logoutAt, s.logoutType = 'FORCED' WHERE s.userId = :userId AND s.isActive = true")
    int deactivateAllUserSessions(@Param("userId") Long userId, @Param("logoutAt") LocalDateTime logoutAt);

    /**
     * Update last activity time for a session.
     *
     * @param sessionId Session ID
     * @param lastActivityAt New activity timestamp
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :lastActivityAt WHERE s.id = :sessionId")
    void updateLastActivity(@Param("sessionId") Long sessionId, @Param("lastActivityAt") LocalDateTime lastActivityAt);

    /**
     * Find all sessions for a user (active and inactive).
     *
     * @param userId User ID
     * @return List of all sessions
     */
    List<UserSession> findByUserIdOrderByLoginAtDesc(Long userId);
}
