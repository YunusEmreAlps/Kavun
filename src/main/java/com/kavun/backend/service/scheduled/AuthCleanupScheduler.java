package com.kavun.backend.service.scheduled;

import com.kavun.backend.persistent.repository.CaptchaRepository;
import com.kavun.backend.persistent.repository.OtpRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service for cleaning up expired authentication-related records.
 * Runs periodic cleanup tasks to remove expired OTP codes, used CAPTCHAs,
 * and other temporary authentication data.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCleanupScheduler {

  private final OtpRepository otpRepository;
  private final CaptchaRepository captchaRepository;

  /**
   * Cleanup expired OTP records.
   * Runs every 10 minutes.
   * Removes OTP records that have expired more than 1 hour ago.
   */
  @Scheduled(cron = "0 */10 * * * *") // Every 10 minutes
  @Transactional
  public void cleanupExpiredOtps() {
    LOG.debug("Starting OTP cleanup task...");

    try {
      // Delete OTPs expired more than 1 hour ago
      Instant threshold = Instant.now().minus(1, ChronoUnit.HOURS);
      int deletedCount = otpRepository.cleanupOldOtps(threshold);

      if (deletedCount > 0) {
        LOG.info("Cleaned up {} expired OTP records", deletedCount);
      } else {
        LOG.debug("No expired OTP records to cleanup");
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup expired OTPs", e);
    }
  }

  /**
   * Cleanup expired and used CAPTCHA records.
   * Runs every 15 minutes.
   * Removes CAPTCHA records that have expired or been used more than 1 hour ago.
   */
  @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
  @Transactional
  public void cleanupExpiredCaptchas() {
    LOG.debug("Starting CAPTCHA cleanup task...");

    try {
      // Delete CAPTCHAs expired or used more than 1 hour ago
      LocalDateTime threshold = LocalDateTime.now().minusHours(1);
      int deletedCount = captchaRepository.cleanupOldCaptchas(threshold);

      if (deletedCount > 0) {
        LOG.info("Cleaned up {} expired/used CAPTCHA records", deletedCount);
      } else {
        LOG.debug("No expired CAPTCHA records to cleanup");
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup expired CAPTCHAs", e);
    }
  }

  /**
   * Deep cleanup of very old authentication records.
   * Runs once daily at 3 AM.
   * Removes all authentication records older than 7 days.
   */
  @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
  @Transactional
  public void deepCleanup() {
    LOG.info("Starting deep cleanup of old authentication records...");

    try {
      // Delete OTPs older than 7 days
      Instant otpThreshold = Instant.now().minus(7, ChronoUnit.DAYS);
      int deletedOtps = otpRepository.cleanupOldOtps(otpThreshold);

      // Delete CAPTCHAs older than 7 days
      LocalDateTime captchaThreshold = LocalDateTime.now().minusDays(7);
      int deletedCaptchas = captchaRepository.cleanupOldCaptchas(captchaThreshold);

      LOG.info("Deep cleanup completed: {} OTPs and {} CAPTCHAs removed",
          deletedOtps, deletedCaptchas);

    } catch (Exception e) {
      LOG.error("Failed to perform deep cleanup", e);
    }
  }

  /**
   * Log cleanup statistics.
   * Runs every hour to provide visibility into cleanup operations.
   */
  @Scheduled(cron = "0 0 * * * *") // Every hour
  public void logCleanupStatistics() {
    LOG.debug("=== AUTH CLEANUP STATISTICS ===");
    LOG.debug("Next OTP cleanup: Every 10 minutes");
    LOG.debug("Next CAPTCHA cleanup: Every 15 minutes");
    LOG.debug("Next deep cleanup: Daily at 3 AM");
    LOG.debug("==============================");
  }
}
