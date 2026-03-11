package com.kavun.backend.persistent.repository;


import com.kavun.backend.persistent.domain.user.Captcha;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository for the Captcha.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface CaptchaRepository extends DataTablesRepository<Captcha, Long> {
    // Find captcha by captchaId and code that is not used and not expired
    @Query("SELECT c FROM Captcha c WHERE c.captchaId = :captchaId AND c.code = :code AND c.used = false AND c.usedAt IS NULL AND c.expiresAt > :now")
    Captcha findValidCaptcha(String captchaId, String code, LocalDateTime now);

    // Delete expired CAPTCHAs (returns count for logging)
    @Modifying
    @Transactional
    @Query("DELETE FROM Captcha c WHERE c.expiresAt < :threshold")
    int deleteExpired(LocalDateTime threshold);

    // Delete old used CAPTCHAs
    @Modifying
    @Transactional
    @Query("DELETE FROM Captcha c WHERE c.used = true AND c.createdAt < :threshold")
    int deleteOldUsed(LocalDateTime threshold);

    // Delete used CAPTCHAs before a certain threshold
    @Modifying
    @Transactional
    @Query("DELETE FROM Captcha c WHERE c.used = true AND c.usedAt < :threshold")
    int deleteUsedBefore(LocalDateTime threshold);

    // Captcha cleanup
    @Modifying
    @Transactional
    @Query("DELETE FROM Captcha c WHERE c.expiresAt < :threshold " + "OR (c.used = true AND (c.createdAt < :threshold OR c.usedAt < :threshold))")
    int cleanupOldCaptchas(LocalDateTime threshold);
}
