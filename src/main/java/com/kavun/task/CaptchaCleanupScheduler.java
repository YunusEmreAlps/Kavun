package com.kavun.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kavun.backend.persistent.repository.CaptchaRepository;

import jakarta.transaction.Transactional;

/**
 * Scheduled task for cleaning up expired and used CAPTCHAs from the database.
 * - Deletes CAPTCHAs that have expired more than 1 hour ago.
 * - Deletes CAPTCHAs that have been used more than 1 hour ago.
 * - Keeps unused valid CAPTCHAs until they expire.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaCleanupScheduler {

    private final CaptchaRepository captchaRepository;

    @Transactional
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void cleanupExpiredCaptchas() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        int deleted = captchaRepository.cleanupOldCaptchas(oneHourAgo);

        if (deleted > 0) {
            LOG.info("CAPTCHA cleanup: {} records deleted", deleted);
        }
    }
}
