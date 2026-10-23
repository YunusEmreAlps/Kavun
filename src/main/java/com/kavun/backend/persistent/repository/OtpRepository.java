package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Otp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for the Otp.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
public interface OtpRepository extends DataTablesRepository<Otp, Long> {

  /**
   * Find an otp by the public id handed to the client as its challenge id.
   *
   * @param publicId the public id
   * @return the otp, if any
   */
  Optional<Otp> findByPublicId(String publicId);

  /**
   * Delete expired OTP records.
   *
   * @param threshold the time before which to delete records
   * @return the number of deleted records
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Otp o WHERE o.expiresAt < :threshold")
  int deleteExpired(LocalDateTime threshold);

  /**
   * Delete inactive OTP records that have expired.
   *
   * @param threshold the creation time before which to delete records
   * @return the number of deleted records
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Otp o WHERE o.active = false AND o.createdAt < :threshold")
  int deleteInactive(LocalDateTime threshold);

  /**
   * Cleanup old OTP records (expired or inactive).
   *
   * @param threshold delete records that expired, or were deactivated and created, before this time
   * @return the number of deleted records
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Otp o WHERE o.expiresAt < :threshold OR (o.active = false AND o.createdAt < :threshold)")
  int cleanupOldOtps(LocalDateTime threshold);
}
