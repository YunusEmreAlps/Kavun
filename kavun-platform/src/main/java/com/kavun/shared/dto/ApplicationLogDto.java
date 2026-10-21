package com.kavun.shared.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationLogDto extends BaseDto {
    private String username;
    private String action;
    private String correlationId;
    private String deviceId;
    private String deviceType;
    private String hostname;
    private String httpStatus;
    private String ipAddress;
    private String logMessage;
    private String requestUrl;
    private String operatingSystem;
    private String browser;
    private String userIpAddress;
    private String userAgent;
}
