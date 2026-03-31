package com.kavun.task;

import com.kavun.backend.persistent.repository.UserSessionRepository;
import com.kavun.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled service for cleaning up expired user sessions.
 * Runs periodically to mark inactive sessions as timed out.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    @Value("${access-token-expiration-in-minutes:60}")
    private long sessionTimeoutMinutes;

    private final UserSessionRepository userSessionRepository;

    /**
     * Finds and expires sessions that have exceeded the timeout duration.
     * Runs every 5 minutes based on SESSION_CLEANUP_CRON.
     *
     * Uses bulk update to avoid optimistic locking failures when multiple
     * transactions modify the same session concurrently.
     */
    @Scheduled(cron = SecurityConstants.SESSION_CLEANUP_CRON)
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now()
                    .minusMinutes(sessionTimeoutMinutes);
            LocalDateTime logoutTime = LocalDateTime.now();

            int expiredCount = userSessionRepository.expireInactiveSessions(cutoffTime, logoutTime);

            if (expiredCount > 0) {
                LOG.info("Expired {} inactive sessions (timeout: {} minutes)",
                        expiredCount,
                        sessionTimeoutMinutes);
            }
        } catch (Exception e) {
            LOG.error("Error during session cleanup: {}", e.getMessage(), e);
        }
    }
}
