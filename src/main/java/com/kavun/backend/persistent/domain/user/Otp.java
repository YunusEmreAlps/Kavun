package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.constant.SecurityConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Instant;

import org.hibernate.annotations.SQLDelete;

import lombok.Getter;
import lombok.Setter;

/**
 * The OTP model for the application.
 *
 * @version 1.0
 * @since 1.0
 */
@Entity
@Getter
@Setter
@Table(name = "otp")
@SQLDelete(sql = "UPDATE otp SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Otp extends BaseEntity implements Serializable {
  @Serial
  private static final long serialVersionUID = 7538542321562810251L;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Column(nullable = false)
  @Size(min = SecurityConstants.OTP_LENGTH, max = SecurityConstants.OTP_LENGTH)
  private String code;

  @Column(nullable = false)
  private String target; // The user's phone number or email address to send the OTP code.

  @Column(nullable = false)
  private Instant expiresAt; // When the OTP expires

  private Instant usedAt; // When the OTP was successfully used

  @Column(nullable = false)
  private Integer failedAttempts = 0; // Track failed attempts

  @Column(nullable = false)
  private Boolean active = true;

  public Otp() {
    this.expiresAt = Instant.now().plusSeconds(SecurityConstants.OTP_DURATION);
  }

  /**
   * Checks if the OTP has expired.
   *
   * @return true if expired, false otherwise
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /**
   * Checks if the OTP is currently valid for use.
   *
   * @return true if active and not expired, false otherwise
   */
  public boolean isValid() {
    return Boolean.TRUE.equals(active) && !isExpired();
  }

  /**
   * Generates a cryptographically secure random OTP code.
   * Uses SecureRandom for enhanced security suitable for authentication purposes.
   *
   * @return OTP code as a string of digits
   */
  public static String generateOtp() {
    StringBuilder otp = new StringBuilder(SecurityConstants.OTP_LENGTH);
    for (int i = 0; i < SecurityConstants.OTP_LENGTH; i++) {
      otp.append(SECURE_RANDOM.nextInt(10));
    }
    return otp.toString();
  }
}
