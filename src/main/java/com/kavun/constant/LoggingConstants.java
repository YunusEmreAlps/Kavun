package com.kavun.constant;

import java.util.Set;

/**
 * Constants for logging configuration and MDC keys.
 * Used by LoggingFilter and other logging components.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 2.0
 */
public final class LoggingConstants {

    private LoggingConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String X_REAL_IP_HEADER = "X-Real-IP";

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_HOSTNAME = "hostname";
    public static final String MDC_IP = "ip";
    public static final String MDC_USER_IP = "userIp";
    public static final String MDC_USER = "user";
    public static final String MDC_URL = "url";
    public static final String MDC_ACTION = "action";
    public static final String MDC_QUERY_PARAMS = "queryParams";
    public static final String MDC_BODY = "body";
    public static final String MDC_PROTOCOL = "protocol";
    public static final String MDC_STATUS = "status";
    public static final String MDC_DURATION = "duration";
    public static final String MDC_RESPONSE_SIZE = "responseSize";
    public static final String MDC_REFERER = "referer";
    public static final String MDC_USER_AGENT = "userAgent";

    public static final String UNKNOWN = "unknown";
    public static final String SYSTEM_USER = "system";
    public static final String NOT_AVAILABLE = "N/A";
    public static final String PROTECTED_DATA = "[PROTECTED]";
    public static final String DEFAULT_VALUE = "-";
    public static final String ZERO = "0";

    public static final int MAX_BODY_LOG_LENGTH = 500;
    public static final int MAX_TRUNCATE_LENGTH = 100;
    public static final String API_PATH_PREFIX = "/api/";
    public static final long SLOW_REQUEST_THRESHOLD_MS = 3000L;
    public static final String LOG_TYPE_HTTP_REQUEST = "HTTP_REQUEST";


    public static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    public static final Set<String> SENSITIVE_PATHS = Set.of(
            "/login", "/auth", "/password", "/token", "/refresh", "/register", "/signup"
    );

    public static final String LOG_LEVEL_ERROR = "ERROR";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_INFO = "INFO";
}
