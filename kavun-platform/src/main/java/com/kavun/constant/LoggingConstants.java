package com.kavun.constant;

import java.util.Set;
import java.util.regex.Pattern;

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
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String DEVICE_ID_HEADER = "X-Device-ID";
    /** Optional client-reported form factor (DESKTOP, TABLET, MOBILE, TV), e.g. for iPadOS Safari. */
    public static final String DEVICE_TYPE_HEADER = "X-Device-Type";
    /** Chromium client hint: "?1" for phones, "?0" for tablets and desktops. */
    public static final String SEC_CH_UA_MOBILE_HEADER = "Sec-CH-UA-Mobile";

    /** Accepted client-supplied correlation IDs; must fit application_logs.correlation_id. */
    public static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,36}$");

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_HOSTNAME = "hostname";
    public static final String MDC_IP = "ip";
    public static final String MDC_USER_IP = "userIp";
    public static final String MDC_USER = "user";
    public static final String MDC_URL = "url";
    public static final String MDC_URI_STEM = "uriStem";
    public static final String MDC_ACTION = "action";
    public static final String MDC_QUERY_PARAMS = "queryParams";
    public static final String MDC_BODY = "body";
    public static final String MDC_PROTOCOL = "protocol";
    public static final String MDC_STATUS = "status";
    public static final String MDC_DURATION = "duration";
    public static final String MDC_RESPONSE_SIZE = "responseSize";
    public static final String MDC_REFERER = "referer";
    public static final String MDC_USER_AGENT = "userAgent";
    public static final String MDC_DEVICE_TYPE = "deviceType";
    public static final String MDC_STATE_BEFORE = "stateBefore";
    public static final String MDC_STATE_AFTER = "stateAfter";
    public static final String MDC_STATE_DIFF = "stateDiff";

    public static final String UNKNOWN = "unknown";
    public static final String SYSTEM_USER = "system";
    public static final String NOT_AVAILABLE = "N/A";
    public static final String PROTECTED_DATA = "[PROTECTED]";
    public static final String DEFAULT_VALUE = "-";
    public static final String ZERO = "0";

    public static final int MAX_BODY_LOG_LENGTH = 500;
    public static final int MAX_TRUNCATE_LENGTH = 100;
    public static final int MAX_DEVICE_ID_LENGTH = 255;
    public static final int MAX_REQUEST_URL_LENGTH = 2048;
    public static final String API_PATH_PREFIX = "/api/";
    public static final long SLOW_REQUEST_THRESHOLD_MS = 3000L;
    public static final String LOG_TYPE_HTTP_REQUEST = "HTTP_REQUEST";
    public static final String LOG_TYPE_SCHEDULED_TASK = "SCHEDULED_TASK";
    public static final String LOG_TYPE_ASYNC_TASK = "ASYNC_TASK";
    public static final String LOG_TYPE_EVENT_TASK = "EVENT_TASK";

    public static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    public static final Set<String> SENSITIVE_PATHS = Set.of("/login", "/reset-password");

    public static final String LOG_LEVEL_ERROR = "ERROR";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_INFO = "INFO";
}
