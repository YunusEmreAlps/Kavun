package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

/**
 * The user sessions model for the application.
 * Tracks active and historical user sessions with device information.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_sessions_device_id", columnList = "device_id"),
    @Index(name = "idx_user_sessions_is_active", columnList = "is_active"),
    @Index(name = "idx_user_sessions_last_activity", columnList = "last_activity_at")
})
@Getter @Setter
@SQLDelete(sql = "UPDATE user_sessions SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserSession extends BaseEntity<Long> implements Serializable {

    @Column(nullable = false)
    private Long userId;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false, name = "login_at")
    private LocalDateTime loginAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(nullable = false, name = "is_active")
    private Boolean isActive = true;

    @Column(name = "refresh_token_hash", length = 255)
    private String refreshTokenHash;

    @Column(name = "logout_type", length = 20)
    private String logoutType; // MANUAL, TIMEOUT, FORCED

    public UserSession() {
        this.loginAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.isActive = true;
    }

    /**
     * Updates the last activity timestamp to current time.
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Marks session as manually logged out.
     */
    public void logout() {
        this.logoutAt = LocalDateTime.now();
        this.isActive = false;
        this.logoutType = "MANUAL";
    }

    /**
     * Marks session as expired due to timeout.
     */
    public void expire() {
        this.logoutAt = LocalDateTime.now();
        this.isActive = false;
        this.logoutType = "TIMEOUT";
    }

    /**
     * Marks session as forcefully terminated.
     */
    public void forceLogout() {
        this.logoutAt = LocalDateTime.now();
        this.isActive = false;
        this.logoutType = "FORCED";
    }

    /**
     * Checks if session is expired based on timeout duration.
     *
     * @param timeoutMinutes Session timeout in minutes
     * @return true if session is expired
     */
    public boolean isExpired(long timeoutMinutes) {
        if (!isActive || logoutAt != null) {
            return true;
        }
        return lastActivityAt.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }
}
