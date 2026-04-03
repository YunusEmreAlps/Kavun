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

    // Find all active sessions for a specific user.
    List<UserSession> findByUserIdAndIsActiveTrueOrderByLoginAtDesc(Long userId);

    // Find active session by user ID and device ID.
    Optional<UserSession> findByUserIdAndDeviceIdAndIsActiveTrue(Long userId, String deviceId);

    // Find all expired sessions based on timeout duration.
    @Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.lastActivityAt < :cutoffTime")
    List<UserSession> findExpiredSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Bulk expire sessions that have exceeded the timeout duration.
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.logoutAt = :logoutAt, s.logoutType = 'TIMEOUT' WHERE s.isActive = true AND s.lastActivityAt < :cutoffTime")
    int expireInactiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("logoutAt") LocalDateTime logoutAt);

    // Count active sessions for a user.
    long countByUserIdAndIsActiveTrue(Long userId);

    // Find session by refresh token hash.
    Optional<UserSession> findByRefreshTokenHashAndIsActiveTrue(String refreshTokenHash);

    // Deactivate all sessions for a user (force logout from all devices).
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.logoutAt = :logoutAt, s.logoutType = 'FORCED' WHERE s.userId = :userId AND s.isActive = true")
    int deactivateAllUserSessions(@Param("userId") Long userId, @Param("logoutAt") LocalDateTime logoutAt);

    // Update last activity time for a session.
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :lastActivityAt WHERE s.id = :sessionId")
    void updateLastActivity(@Param("sessionId") Long sessionId, @Param("lastActivityAt") LocalDateTime lastActivityAt);

    // Find all sessions for a user (active and inactive).
    List<UserSession> findByUserIdOrderByLoginAtDesc(Long userId);

    // Monthly total session duration analytics for all users.
    @Query(value = """
            SELECT DATE_TRUNC('month', login_at) AS month, SUM(EXTRACT(EPOCH FROM (COALESCE(logout_at, CURRENT_TIMESTAMP) - login_at))) AS total_duration
            FROM user_sessions
            WHERE (deleted IS NULL OR deleted = false) AND EXTRACT(YEAR FROM login_at) = :year
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countMonthlySessionDurations(@Param("year") Integer year);
}
