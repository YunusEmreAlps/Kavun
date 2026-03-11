package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Captcha entity for authentication security.
 * Stores generated CAPTCHA codes with expiry and usage tracking.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "captcha", indexes = {
    @Index(name = "idx_captcha_id", columnList = "captcha_id"),
    @Index(name = "idx_used_created", columnList = "used, created_at"),
    @Index(name = "idx_expires", columnList = "expires_at")
})
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Captcha extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "captcha_id", nullable = false, unique = true, length = 100, updatable = false)
    @EqualsAndHashCode.Include
    private String captchaId;

    @Column(name = "code", nullable = false, length = 5, updatable = false)
    private String code;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false, columnDefinition = "boolean default false")
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(5); // 5 minute default expiry
        }
    }

    /**
     * Check if CAPTCHA has expired.
     *
     * @return true if current time is after expiry time
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Validate CAPTCHA code against user input.
     *
     * @param inputCode user-provided CAPTCHA text
     * @return true if valid (not used, not expired, and code matches)
     */
    public boolean isValid(String inputCode) {
        return !used &&
               !isExpired() &&
               code.equalsIgnoreCase(inputCode);
    }
}
