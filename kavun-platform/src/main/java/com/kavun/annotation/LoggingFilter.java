package com.kavun.annotation;

import com.kavun.backend.persistent.context.HttpLogContext;
import com.kavun.backend.service.DeviceDetectionService;
import com.kavun.backend.service.siem.ApplicationLogService;
import com.kavun.shared.util.MaskPasswordUtils;
import com.kavun.shared.util.core.NetworkUtils;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.shared.util.core.StringUtils;
import com.kavun.shared.util.core.TenantContext;

import jakarta.persistence.EntityManager;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import static com.kavun.constant.LoggingConstants.*;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

/**
 * Production-grade HTTP request/response logging filter.
 * Captures request metadata, body content, and access logs for SIEM
 * integration.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

  private final EntityManager entityManager;
  private final DeviceDetectionService deviceDetectionService;
  private final ApplicationLogService applicationLogService;

  /** Cached server info (computed once at startup) */
  private static final String CACHED_HOSTNAME = NetworkUtils.resolveHostname();
  private static final String CACHED_IP = NetworkUtils.resolveIp();

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    long startTime = System.currentTimeMillis();

    Map<String, String> previousMdc = MDC.getCopyOfContextMap();

    // Generate or extract correlation ID for distributed tracing
    String correlationId = extractOrGenerateCorrelationId(request);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    // Cache request body if needed (must be done before reading)
    CachedBodyHttpServletRequest cachedRequest = shouldCacheBody(request)
        ? new CachedBodyHttpServletRequest(request)
        : null;
    HttpServletRequest requestToUse = cachedRequest != null ? cachedRequest : request;

    Throwable failure = null;
    try {
      populateMdc(request, cachedRequest, correlationId);
      filterChain.doFilter(requestToUse, response);
    } catch (ServletException | IOException | RuntimeException | Error e) {
      failure = e;
      throw e;
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      logAccessAndPersist(request, response, resolveStatus(response, failure), cachedRequest,
          correlationId, duration);
      restoreMdc(previousMdc);
    }
  }

  private int resolveStatus(HttpServletResponse response, Throwable failure) {
    int status = response.getStatus();
    return failure != null && status < HttpServletResponse.SC_BAD_REQUEST
        ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        : status;
  }

  private void restoreMdc(Map<String, String> previousMdc) {
    if (previousMdc == null) {
      MDC.clear();
    } else {
      MDC.setContextMap(previousMdc);
    }
  }

  private String extractOrGenerateCorrelationId(HttpServletRequest request) {
    for (String header : new String[] {CORRELATION_ID_HEADER, REQUEST_ID_HEADER}) {
      String candidate = request.getHeader(header);
      if (candidate != null && CORRELATION_ID_PATTERN.matcher(candidate).matches()) {
        return candidate;
      }
    }
    return UUID.randomUUID().toString();
  }

  /**
   * Populates MDC with request context for structured logging.
   */
  private void populateMdc(HttpServletRequest request, CachedBodyHttpServletRequest cachedRequest,
      String correlationId) {
    MDC.put(MDC_CORRELATION_ID, correlationId);
    MDC.put(MDC_HOSTNAME, CACHED_HOSTNAME);
    MDC.put(MDC_IP, CACHED_IP);
    MDC.put(MDC_USER_IP, extractUserIp(request));
    MDC.put(MDC_USER, extractUsername());
    MDC.put(MDC_URL, request.getRequestURL().toString());
    MDC.put(MDC_URI_STEM, request.getRequestURI());
    MDC.put(MDC_ACTION, request.getMethod());
    MDC.put(MDC_QUERY_PARAMS, StringUtils.nullSafe(request.getQueryString()));

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
      int status, CachedBodyHttpServletRequest cachedRequest,
      String correlationId, long duration) {
    String path = request.getRequestURI();
    DeviceDetectionService.DeviceInfo deviceInfo = deviceDetectionService.detect(request);

    // Update MDC with response info; the user may have been authenticated during the request
    MDC.put(MDC_USER, extractUsername());
    MDC.put(MDC_PROTOCOL, request.getProtocol());
    MDC.put(MDC_STATUS, String.valueOf(status));
    MDC.put(MDC_DURATION, String.valueOf(duration));
    MDC.put(MDC_RESPONSE_SIZE, StringUtils.nullSafe(response.getHeader("Content-Length"), ZERO));
    MDC.put(MDC_REFERER, StringUtils.nullSafe(request.getHeader("Referer"), DEFAULT_VALUE));
    MDC.put(MDC_USER_AGENT, StringUtils.nullSafe(request.getHeader(USER_AGENT_HEADER), DEFAULT_VALUE));
    MDC.put(MDC_DEVICE_TYPE, deviceInfo.getDeviceType());

    // Log access with duration
    if (duration > SLOW_REQUEST_THRESHOLD_MS) {
      LOG.warn("Slow request: {} {} completed in {} ms (threshold: {} ms)",
          request.getMethod(), path, duration, SLOW_REQUEST_THRESHOLD_MS);
    } else {
      LOG.info("Request completed in {} ms", duration);
    }

    // Persist to database for API requests
    if (path.startsWith(API_PATH_PREFIX)) {
      persistApplicationLog(request, status, deviceInfo, cachedRequest, correlationId, duration);
    }
  }

  /**
   * Persists application log to database asynchronously.
   */
  private void persistApplicationLog(HttpServletRequest request, int status,
      DeviceDetectionService.DeviceInfo deviceInfo, CachedBodyHttpServletRequest cachedRequest,
      String correlationId, long duration) {
    try {
      String requestBody = (cachedRequest != null && shouldLogBody(request))
          ? sanitizeBody(cachedRequest.getBody(), request.getRequestURI())
          : null;

      String userAgent = request.getHeader(USER_AGENT_HEADER);

      // Get device ID from header if client provided it
      String deviceId = request.getHeader(DEVICE_ID_HEADER);

      applicationLogService.persistHttpLog(new HttpLogContext(
          TenantContext.current(),
          correlationId,
          getClass().getName(),
          buildLogMessage(request.getMethod(), request.getRequestURI(), duration),
          extractUserIp(request),
          extractUsername(),
          extractUserPublicId(),
          request.getRequestURL().toString(),
          request.getMethod(),
          request.getQueryString(),
          requestBody,
          duration,
          status,
          deviceId,
          deviceInfo.getDeviceType(),
          deviceInfo.getOperatingSystem(),
          deviceInfo.getBrowser(),
          userAgent,
          MDC.get(MDC_STATE_BEFORE),
          MDC.get(MDC_STATE_AFTER),
          MDC.get(MDC_STATE_DIFF)));

    } catch (Exception e) {
      LOG.warn("Failed to create application log: {}", e.getMessage());
    }
  }

  /**
   * Attempts to extract an entity ID from method arguments for logging purposes.
   */
  public Object extractEntityId(Object[] args) {
    for (Object arg : args) {
      if (arg instanceof Long || arg instanceof Integer || arg instanceof java.util.UUID) {
        return arg;
      }
      try {
        var field = arg.getClass().getDeclaredField("id");
        field.setAccessible(true);
        Object val = field.get(arg);
        if (val instanceof Long || val instanceof Integer || val instanceof java.util.UUID) {
          return val;
        }
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  public Map<String, Object> snapshotEntity(Class<?> entityClass, Object entityId) {
    try {
      if (entityClass == null) {
        LOG.warn("Entity class is null, cannot snapshot");
        return Map.of();
      }

      Object entity = entityManager.find(entityClass, entityId);
      if (entity == null) {
        LOG.warn("Entity not found for class {} with ID: {}", entityClass.getSimpleName(), entityId);
        return Map.of();
      }

      Map<String, Object> snapshot = new LinkedHashMap<>();
      for (Field field : entity.getClass().getDeclaredFields()) {
        if (field.isAnnotationPresent(Transient.class))
          continue;
        if (field.isAnnotationPresent(OneToMany.class))
          continue;
        if (field.isAnnotationPresent(ManyToMany.class))
          continue;
        field.setAccessible(true);
        Object value = field.get(entity);
        if (value != null && MaskPasswordUtils.isSensitiveKey(field.getName())) {
          value = MaskPasswordUtils.MASK;
        }
        snapshot.put(field.getName(), value);
      }
      LOG.debug("Snapshot created for {} with {} fields", entityClass.getSimpleName(), snapshot.size());
      return snapshot;
    } catch (Exception e) {
      LOG.error("Snapshot alınamadı: {}", e.getMessage(), e);
      return Map.of();
    }
  }

  /**
   * Builds a diff string showing changed fields between two snapshots.
   * (value=old->Newvalue=new)
   */
  public String buildDiff(Map<String, Object> before, Map<String, Object> after) {
    return after.entrySet().stream()
        .filter(e -> {
          Object oldVal = before.get(e.getKey());
          return !Objects.equals(oldVal, e.getValue());
        })
        .map(e -> String.format("%s=%s->New%s=%s",
            StringUtils.capitalize(e.getKey()),
            before.get(e.getKey()),
            StringUtils.capitalize(e.getKey()),
            e.getValue()))
        .collect(Collectors.joining(", "));
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
    if (path == null)
      return false;
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

    /*
     * if (isSensitivePath(path)) {
     * return PROTECTED_DATA;
     * }
     */

    // Mask passwords and sensitive data
    String sanitized = MaskPasswordUtils.mask(body);

    // Normalize whitespace for single-line logging
    sanitized = sanitized.replaceAll("[\\r\\n]+", " ")
        .replaceAll("\\s+", " ")
        .trim();

    return StringUtils.truncate(sanitized, MAX_BODY_LOG_LENGTH);
  }

  /**
   * Builds log message for database persistence.
   */
  private String buildLogMessage(String action, String path, long duration) {
    return String.format("%s %s completed in %d ms", action, path, duration);
  }

  private String extractUserIp(HttpServletRequest request) {
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
}
