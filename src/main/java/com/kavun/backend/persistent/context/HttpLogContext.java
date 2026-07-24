package com.kavun.backend.persistent.context;

public record HttpLogContext(
    String correlationId, String loggerName, String logMessage,
    String userIp, String username, String userId,
    String requestUrl, String action, String requestParams,
    String requestBody, long durationMs, int httpStatus,
    String deviceId, String deviceType, String operatingSystem,
    String browser, String userAgent) {
}
