package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.constant.SecurityConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Random;
import java.util.stream.Collectors;
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
public class Otp extends BaseEntity<Long> implements Serializable {
  @Serial private static final long serialVersionUID = 7538542321562810251L;

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
   * Generate X-digit random OTP code.
   *
   * @return the generated OTP code
   */
  public static String generateOtp() {
    Random random = new Random();
    return random
        .ints(SecurityConstants.OTP_LENGTH, 0, 10) // 0-9
        .mapToObj(Integer::toString)
        .collect(Collectors.joining());
  }
}
