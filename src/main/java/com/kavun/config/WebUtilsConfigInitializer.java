package com.kavun.config;

import com.kavun.shared.util.core.WebUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bridges the 'system.url' property into {@link WebUtils}, which is a static utility class and
 * so cannot be injected into directly. Required so links built for emails sent from a
 * background thread (no HTTP request bound to it) don't depend on Spring's
 * RequestContextHolder, which is only ever bound to the original request-handling thread.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class WebUtilsConfigInitializer {

  @Value("${system.url}")
  private String systemUrl;

  @PostConstruct
  private void init() {
    WebUtils.setSystemUrl(systemUrl);
  }
}
