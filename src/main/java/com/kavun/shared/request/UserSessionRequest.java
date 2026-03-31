package com.kavun.shared.request;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionRequest extends BaseRequest {
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
}
