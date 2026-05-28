package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Otp;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for the Otp.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface OtpRepository extends DataTablesRepository<Otp, Long> {

  /**
   * Find otp by target.
   *
   * @param target the target to email or sms
   * @return Otp found.
   */
  Otp findByTarget(String target);

  /**
   * Find otp by target and code.
   *
   * @param target the target to email or sms
   * @param code the otp code to validate
   * @return Otp found.
   */
  Otp findByTargetAndCode(String target, String code);

  /**
   * Get active codes for the target.
   *
   * @param target the target to email or sms
   * @param status the status of the otp
   * @return Otp found.
   */
  List<Otp> findByTargetAndActive(String target, Boolean status);

  /**
   * Get active codes for the target.
   *
   * @param target the target to email or sms
   * @param expiresAt the expiration date of the otp
   * @param status the status of the otp
   * @return Otp found.
   */
  List<Otp> findByTargetAndExpiresAtAfterAndActive(
      String target, Instant expiresAt, Boolean status);

  /**
   * Check if there's an active, unexpired OTP for the target
   *
   * @param target the target to email or sms
   * @param code the otp code to validate
   * @param expiresAt the expiration date of the otp
   * @param status the status of the otp
   * @return Otp found.
   */
  Otp findByTargetAndCodeAndExpiresAtAfterAndActive(
      String target, String code, Instant expiresAt, Boolean status);

  /**
   * Delete expired OTP records.
   *
   * @param threshold the threshold instant before which to delete records
   * @return the number of deleted records
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Otp o WHERE o.expiresAt < :threshold")
  int deleteExpired(Instant threshold);

  /**
   * Delete inactive OTP records that have expired.
   *
   * @param threshold the threshold instant before which to delete records
   * @return the number of deleted records
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Otp o WHERE o.active = false AND o.createdAt < :threshold")
  int deleteInactive(Instant threshold);

  /**
   * Cleanup old OTP records (expired or inactive).
   *
   * @param threshold the threshold instant before which to delete records
   * @return the number of deleted records
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Otp o WHERE o.expiresAt < :threshold OR (o.active = false AND o.createdAt < :threshold)")
  int cleanupOldOtps(Instant threshold);
}
