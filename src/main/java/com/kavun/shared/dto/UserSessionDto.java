package com.kavun.shared.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionDto extends BaseDto {
    private String userId;
    private String deviceId;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    private String userAgent;
    private String ipAddress;
    private LocalDateTime loginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime logoutAt;
    private Boolean isActive;
    private Long sessionDurationSeconds;

    // Calculates session duration in seconds based on login and logout times.
    public void calculateSessionDuration() {
        if (loginAt != null && logoutAt != null) {
            this.sessionDurationSeconds = java.time.Duration.between(loginAt, logoutAt).getSeconds();
        } else if (loginAt != null && lastActivityAt != null) {
            this.sessionDurationSeconds = java.time.Duration.between(loginAt, lastActivityAt).getSeconds();
        } else {
            this.sessionDurationSeconds = 0L;
        }
    }
}
