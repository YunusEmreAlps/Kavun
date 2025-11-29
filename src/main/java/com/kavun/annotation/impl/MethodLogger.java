package com.kavun.annotation.impl;

import com.kavun.annotation.Loggable;
import com.kavun.shared.util.MaskPasswordUtils;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

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
      logEntry(level, method, joinPoint.getArgs());
    }

    final long start = System.nanoTime();

    try {
      Object response = joinPoint.proceed();
      final long durationMs = (System.nanoTime() - start) / 1_000_000;

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

  private void logEntry(Level level, String method, Object[] args) {
    String maskedArgs = MaskPasswordUtils.maskPasswordJson(Arrays.toString(args)).toString();
    logAtLevel(level, ENTRY_FORMAT, method, maskedArgs);
  }

  private void logExit(Level level, String method, Object response, long durationMs) {
    String truncatedResponse = truncateResponse(response);
    logAtLevel(level, EXIT_FORMAT, method, truncatedResponse, durationMs);
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

  private String truncateResponse(Object response) {
    if (response == null) {
      return "null";
    }
    String str = response.toString();
    return str.length() > MAX_RESPONSE_LENGTH
        ? str.substring(0, MAX_RESPONSE_LENGTH) + "... (truncated)"
        : str;
  }
}
