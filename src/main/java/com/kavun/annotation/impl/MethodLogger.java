package com.kavun.annotation.impl;

import com.kavun.annotation.Loggable;
import com.kavun.shared.util.MaskPasswordUtils;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Ensures that method calls can be logged with entry-exit logs in console or log file.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
public class MethodLogger {

  /**
   * - visibility modifier is * (public, protected or private) - name is * (any name); - arguments
   * are .. (any arguments); and - is annotated with @Loggable.
   *
   * @param joinPoint the joinPoint
   * @return the log object
   * @throws Throwable if an error occurs
   */
  @Around("execution(* *(..)) && @annotation(loggable)")
  public Object log(final ProceedingJoinPoint joinPoint, final Loggable loggable) throws Throwable {

    var method = joinPoint.toShortString();
    var start = System.currentTimeMillis();

    switchStartingLogger(loggable.level(), method, joinPoint.getArgs());
    Object response = joinPoint.proceed();

    // if a response object is ignored, don't include response data.
    if (loggable.ignoreResponseData()) {
      switchFinishingLogger(loggable.level(), method, "{...}", start);
    } else {
      switchFinishingLogger(loggable.level(), method, response, start);
    }

    return response;
  }

  private void switchStartingLogger(final String level, final String method, final Object args) {
    final String format = "=> Starting -  {} args: {}";

    switch (level) {
      case "warn" -> LOG.warn(format, method, MaskPasswordUtils.maskPasswordJson(args));
      case "error" -> LOG.error(format, method, MaskPasswordUtils.maskPasswordJson(args));
      case "debug" -> LOG.debug(format, method, MaskPasswordUtils.maskPasswordJson(args));
      case "trace" -> LOG.trace(format, method, MaskPasswordUtils.maskPasswordJson(args));
      default -> LOG.info(format, method, MaskPasswordUtils.maskPasswordJson(args));
    }
  }

  private void switchFinishingLogger(String level, String method, Object response, long start) {
    long duration = System.currentTimeMillis() - start;
    final String format = "<= {} : {} - Finished, duration: {} ms";

    // Log performance warning if method execution is too slow
    if (duration > 3000) {
      LOG.warn("Slow method execution: {} took {} ms", method, duration);
    }

    switch (level) {
      case "warn" -> LOG.warn(format, method, truncateResponse(response), duration);
      case "error" -> LOG.error(format, method, truncateResponse(response), duration);
      case "debug" -> LOG.debug(format, method, truncateResponse(response), duration);
      case "trace" -> LOG.trace(format, method, truncateResponse(response), duration);
      default -> LOG.info(format, method, truncateResponse(response), duration);
    }
  }

  // Truncate large response objects to avoid log bloat
  private Object truncateResponse(Object response) {
    if (response == null) {
      return "null";
    }

    String responseStr = response.toString();
    if (responseStr.length() > 500) {
      return responseStr.substring(0, 500) + "... (truncated)";
    }

    return response;
  }
}
