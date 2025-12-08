package com.kavun.annotation;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import com.kavun.backend.persistent.repository.ApplicationLogRepository;
import com.kavun.shared.util.MaskPasswordUtils;
import com.kavun.shared.util.core.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import static com.kavun.constant.LoggingConstants.*;

/**
 * Production-grade HTTP request/response logging filter.
 * Captures request metadata, body content, and access logs for SIEM integration.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final ApplicationLogRepository applicationLogRepository;

    /** Cached server info (computed once at startup) */
    private static final String CACHED_HOSTNAME = resolveHostname();
    private static final String CACHED_IP = resolveIp();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Generate or extract correlation ID for distributed tracing
        String correlationId = extractOrGenerateCorrelationId(request);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Cache request body if needed (must be done before reading)
        CachedBodyHttpServletRequest cachedRequest = shouldCacheBody(request)
                ? new CachedBodyHttpServletRequest(request)
                : null;
        HttpServletRequest requestToUse = cachedRequest != null ? cachedRequest : request;

        try {
            populateMdc(request, cachedRequest, correlationId);
            filterChain.doFilter(requestToUse, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logAccessAndPersist(request, response, cachedRequest, correlationId, duration);
            MDC.clear();
        }
    }

    /**
     * Extracts correlation ID from request headers or generates a new one.
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(REQUEST_ID_HEADER);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Populates MDC with request context for structured logging.
     */
    private void populateMdc(HttpServletRequest request, CachedBodyHttpServletRequest cachedRequest, String correlationId) {
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_HOSTNAME, CACHED_HOSTNAME);
        MDC.put(MDC_IP, CACHED_IP);
        MDC.put(MDC_USER_IP, extractUserIp(request));
        MDC.put(MDC_USER, extractUsername());
        MDC.put(MDC_URL, request.getRequestURL().toString());
        MDC.put(MDC_ACTION, request.getMethod());
        MDC.put(MDC_QUERY_PARAMS, nullSafe(request.getQueryString()));

        // Add sanitized body to MDC if available
        if (cachedRequest != null && shouldLogBody(request)) {
            String sanitizedBody = sanitizeBody(cachedRequest.getBody(), request.getRequestURI());
            MDC.put(MDC_BODY, sanitizedBody);
        }
    }

    /**
     * Logs access information and persists to database.
     */
    private void logAccessAndPersist(HttpServletRequest request, HttpServletResponse response,
                                      CachedBodyHttpServletRequest cachedRequest,
                                      String correlationId, long duration) {
        String path = request.getRequestURI();
        int status = response.getStatus();

        // Update MDC with response info
        MDC.put(MDC_PROTOCOL, request.getProtocol());
        MDC.put(MDC_STATUS, String.valueOf(status));
        MDC.put(MDC_DURATION, String.valueOf(duration));
        MDC.put(MDC_RESPONSE_SIZE, nullSafe(response.getHeader("Content-Length"), ZERO));
        MDC.put(MDC_REFERER, nullSafe(request.getHeader("Referer"), DEFAULT_VALUE));
        MDC.put(MDC_USER_AGENT, nullSafe(request.getHeader("User-Agent"), DEFAULT_VALUE));

        // Log access with duration
        if (duration > SLOW_REQUEST_THRESHOLD_MS) {
            LOG.warn("Slow request: {} {} completed in {} ms (threshold: {} ms)",
                    request.getMethod(), path, duration, SLOW_REQUEST_THRESHOLD_MS);
        } else {
            LOG.info("Request completed in {} ms", duration);
        }

        // Persist to database for API requests
        if (path.startsWith(API_PATH_PREFIX)) {
            persistApplicationLog(request, response, cachedRequest, correlationId, duration);
        }
    }

    /**
     * Persists application log to database asynchronously.
     */
    private void persistApplicationLog(HttpServletRequest request, HttpServletResponse response,
                                        CachedBodyHttpServletRequest cachedRequest,
                                        String correlationId, long duration) {
        try {
            String requestBody = (cachedRequest != null && shouldLogBody(request))
                    ? sanitizeBody(cachedRequest.getBody(), request.getRequestURI())
                    : null;

            ApplicationLog applicationLog = ApplicationLog.builder()
                    .correlationId(correlationId)
                    .logLevel(determineLogLevel(response.getStatus()))
                    .threadName(Thread.currentThread().getName())
                    .loggerName(getClass().getName())
                    .logMessage(buildLogMessage(request.getMethod(), request.getRequestURI(), duration))
                    .hostname(CACHED_HOSTNAME)
                    .ip(CACHED_IP)
                    .logType(LOG_TYPE_HTTP_REQUEST)
                    .userIpAddress(extractUserIp(request))
                    .username(extractUserPublicId())
                    .requestUrl(request.getRequestURL().toString())
                    .action(request.getMethod())
                    .requestParams(request.getQueryString())
                    .requestBody(requestBody)
                    .durationMs(duration)
                    .httpStatus(response.getStatus())
                    .build();

            saveLogAsync(applicationLog);
        } catch (Exception e) {
            LOG.warn("Failed to create application log: {}", e.getMessage());
        }
    }

    /**
     * Saves log to database with error handling.
     */
    private void saveLogAsync(ApplicationLog applicationLog) {
        try {
            applicationLogRepository.save(applicationLog);
        } catch (Exception e) {
            LOG.warn("Failed to persist application log: {}", e.getMessage());
        }
    }

    /**
     * Determines log level based on HTTP status code.
     */
    private String determineLogLevel(int status) {
        if (status >= 500) return LOG_LEVEL_ERROR;
        if (status >= 400) return LOG_LEVEL_WARN;
        return LOG_LEVEL_INFO;
    }

    /**
     * Determines if request body should be cached.
     */
    private boolean shouldCacheBody(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();

        return BODY_METHODS.contains(method.toUpperCase())
                && contentType != null
                && contentType.contains("application/json");
    }

    /**
     * Determines if request body should be logged.
     */
    private boolean shouldLogBody(HttpServletRequest request) {
        return BODY_METHODS.contains(request.getMethod().toUpperCase());
    }

    /**
     * Checks if the path contains sensitive data.
     */
    private boolean isSensitivePath(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return SENSITIVE_PATHS.stream().anyMatch(lowerPath::contains);
    }

    /**
     * Sanitizes request body for logging.
     */
    private String sanitizeBody(String body, String path) {
        if (body == null || body.isBlank()) {
            return NOT_AVAILABLE;
        }

        if (isSensitivePath(path)) {
            return PROTECTED_DATA;
        }

        // Mask passwords and sensitive data
        Object masked = MaskPasswordUtils.maskPasswordJson(body);
        String sanitized = masked != null ? masked.toString() : body;

        // Normalize whitespace for single-line logging
        sanitized = sanitized.replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return truncate(sanitized, MAX_BODY_LOG_LENGTH);
    }

    /**
     * Builds log message for database persistence.
     */
    private String buildLogMessage(String action, String path, long duration) {
        return String.format("%s %s completed in %d ms", action, path, duration);
    }

    private static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return UNKNOWN;
        }
    }

    private static String resolveIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return UNKNOWN;
        }
    }

    /**
     * Extracts user IP from request, handling proxies.
     */
    private String extractUserIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (standard for proxies/load balancers)
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header (nginx)
        String realIp = request.getHeader(X_REAL_IP_HEADER);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String extractUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (isAuthenticated(auth)) {
                return SecurityUtils.getAuthenticatedUserDetails().getUsername();
            }
        } catch (Exception e) {
            LOG.trace("Could not extract username: {}", e.getMessage());
        }
        return SYSTEM_USER;
    }

    private String extractUserPublicId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (isAuthenticated(auth)) {
                return SecurityUtils.getAuthenticatedUserDetails().getPublicId();
            }
        } catch (Exception e) {
            LOG.trace("Could not extract user publicId: {}", e.getMessage());
        }
        return SYSTEM_USER;
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    // ==================== String Utilities ====================

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String nullSafe(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
