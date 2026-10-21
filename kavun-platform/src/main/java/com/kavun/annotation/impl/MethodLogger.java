package com.kavun.annotation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kavun.annotation.Loggable;
import com.kavun.annotation.LoggingFilter;
import com.kavun.backend.persistent.context.MethodLogContext;
import com.kavun.backend.service.siem.ApplicationLogService;
import com.kavun.shared.util.MaskPasswordUtils;
import com.kavun.shared.util.core.StringUtils;
import com.kavun.shared.util.core.TenantContext;

import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static com.kavun.constant.LoggingConstants.*;

/**
 * Aspect for logging method entry, exit, and execution time for methods
 * annotated with {@link Loggable}.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
public class MethodLogger {

  private static final int MAX_RESPONSE_LENGTH = 500;
  private static final long SLOW_THRESHOLD_MS = 3000L;
  private static final String ENTRY_FORMAT = "=> Starting - {} args: {}";
  private static final String EXIT_FORMAT = "<= {} : {} - Finished, duration: {} ms";

  private final LoggingFilter loggingFilter;
  private final ObjectMapper objectMapper;
  private final ApplicationLogService applicationLogService;

  MethodLogger(LoggingFilter loggingFilter, ObjectMapper objectMapper, ApplicationLogService applicationLogService) {
    this.loggingFilter = loggingFilter;
    this.objectMapper = objectMapper;
    this.applicationLogService = applicationLogService;
  }

  /**
   * Intercepts and logs methods annotated with {@link Loggable}.
   *
   * @param joinPoint AOP join point
   * @param loggable  annotation info
   * @return method return value
   * @throws Throwable errors occurring during method execution
   */
  @Around("execution(* *(..)) && @annotation(loggable)")
  public Object log(final ProceedingJoinPoint joinPoint, final Loggable loggable) throws Throwable {
    final String method = joinPoint.toShortString();
    final Level level = parseLevel(loggable.level());

    // Entry log - skip if log level is not enabled
    if (isLevelEnabled(level)) {
      logEntry(level, method, joinPoint);
    }

    final long start = System.nanoTime();

    try {
      Object entityId = loggingFilter.extractEntityId(joinPoint.getArgs());

      Map<String, Object> before = (entityId != null && loggable.entityClass() != Object.class)
          ? loggingFilter.snapshotEntity(loggable.entityClass(), entityId)
          : Map.of();

      Object response = joinPoint.proceed();

      Map<String, Object> after = (entityId != null && loggable.entityClass() != Object.class)
          ? loggingFilter.snapshotEntity(loggable.entityClass(), entityId)
          : Map.of();

      boolean isHttpContext = isHttpContext();
      final long durationMs = (System.nanoTime() - start) / 1_000_000;

      String diff = (!before.isEmpty() && !after.isEmpty())
          ? loggingFilter.buildDiff(before, after)
          : "";
      if (isHttpContext) {
        if (!diff.isEmpty() && loggable.entityClass() != Object.class) {
          LOG.info("Entity changes: {}", diff);
          MDC.put(MDC_STATE_BEFORE, objectMapper.writeValueAsString(before));
          MDC.put(MDC_STATE_AFTER, objectMapper.writeValueAsString(after));
          MDC.put(MDC_STATE_DIFF, diff);
        }
      } else {
        applicationLogService.persistMethodLog(new MethodLogContext(
            TenantContext.current(),
            method,
            resolveLogType(),
            durationMs,
            before.isEmpty() ? null : objectMapper.writeValueAsString(before),
            after.isEmpty() ? null : objectMapper.writeValueAsString(after),
            diff.isEmpty() ? null : diff));
      }

      // Exit log
      if (isLevelEnabled(level)) {
        Object loggedResponse = loggable.ignoreResponseData() ? "{...}" : response;
        logExit(level, method, loggedResponse, durationMs);
      }

      // Slow method warning
      if (durationMs > SLOW_THRESHOLD_MS) {
        LOG.warn("Slow method: {} took {} ms (threshold: {} ms)", method, durationMs, SLOW_THRESHOLD_MS);
      }

      return response;
    } catch (Exception e) {
      LOG.error("Exception in method: {} with message: {}", method, e.getMessage());
      throw e;
    }
  }

  private boolean isHttpContext() {
    try {
      return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()) != null;
    } catch (Exception e) {
      return false;
    }
  }

  private Level parseLevel(String level) {
    try {
      return Level.valueOf(level.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Level.INFO;
    }
  }

  private boolean isLevelEnabled(Level level) {
    return switch (level) {
      case TRACE -> LOG.isTraceEnabled();
      case DEBUG -> LOG.isDebugEnabled();
      case WARN -> LOG.isWarnEnabled();
      case ERROR -> LOG.isErrorEnabled();
      default -> LOG.isInfoEnabled();
    };
  }

  private void logEntry(Level level, String method, ProceedingJoinPoint joinPoint) {
    String[] names = joinPoint.getSignature() instanceof MethodSignature signature
        ? signature.getParameterNames()
        : null;
    logAtLevel(level, ENTRY_FORMAT, method, MaskPasswordUtils.maskArguments(names, joinPoint.getArgs()));
  }

  private void logExit(Level level, String method, Object response, long durationMs) {
    String maskedResponse = MaskPasswordUtils.mask(response);
    logAtLevel(level, EXIT_FORMAT, method, StringUtils.truncate(maskedResponse, MAX_RESPONSE_LENGTH), durationMs);
  }

  private void logAtLevel(Level level, String format, Object... args) {
    switch (level) {
      case TRACE -> LOG.trace(format, args);
      case DEBUG -> LOG.debug(format, args);
      case WARN -> LOG.warn(format, args);
      case ERROR -> LOG.error(format, args);
      default -> LOG.info(format, args);
    }
  }

  private String resolveLogType() {
    String threadName = Thread.currentThread().getName().toLowerCase(Locale.ROOT);
    if (threadName.contains("scheduling"))
      return LOG_TYPE_SCHEDULED_TASK;
    if (threadName.contains("async"))
      return LOG_TYPE_ASYNC_TASK;
    if (threadName.contains("event"))
      return LOG_TYPE_EVENT_TASK;
    return LOG_TYPE_HTTP_REQUEST;
  }
}
