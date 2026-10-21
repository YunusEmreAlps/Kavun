package com.kavun.shared.util.core;

import com.kavun.constant.ErrorConstants;
import com.kavun.constant.HomeConstants;
import com.kavun.constant.email.EmailConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This utility class holds all common methods used in the web layer.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class WebUtils {

  public static final String TOKEN = "token";

  // Populated at startup from 'system.url' by WebUtilsConfigInitializer. Building links from
  // this configured base URL - rather than from the current HTTP request's thread-local
  // context - is required for any caller that runs off the original request thread (e.g. an
  // email sent from CompletableFuture.runAsync()/@Async): ServletUriComponentsBuilder needs
  // Spring's RequestContextHolder to be bound to the CURRENT thread, which it never is on a
  // background thread, and throws "No current ServletRequestAttributes" there.
  private static volatile String systemUrl;

  private WebUtils() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }

  /**
   * Called once at startup by WebUtilsConfigInitializer.
   *
   * @param url the configured 'system.url' value
   */
  public static void setSystemUrl(String url) {
    systemUrl = url;
  }

  /**
   * Generates a uri dynamically by constructing url.
   *
   * @param path the custom path
   * @param publicUserId the publicUserId
   * @return a dynamically formulated uri
   */
  public static String getGenericUri(String path, String publicUserId) {
    return uriBuilder(path).queryParam(TOKEN, publicUserId).build().toUriString();
  }

  /**
   * Generates a uri dynamically by constructing url.
   *
   * @param path the custom path
   * @return a dynamically formulated uri
   */
  public static String getGenericUri(String path) {
    return uriBuilder(path).build().toUriString();
  }

  private static UriComponentsBuilder uriBuilder(String path) {
    if (StringUtils.isNotBlank(systemUrl)) {
      return UriComponentsBuilder.fromUriString(systemUrl).path(path);
    }
    // Fallback for callers still running on the original request thread before
    // WebUtilsConfigInitializer has run (or in a context with no 'system.url' at all).
    return ServletUriComponentsBuilder.fromCurrentContextPath().path(path);
  }

  /**
   * Get general links used in email definitions.
   *
   * @return default links
   */
  public static Map<String, String> getDefaultEmailUrls() {
    Map<String, String> links = new ConcurrentHashMap<>();
    links.put(EmailConstants.HOME_LINK, getGenericUri(HomeConstants.INDEX_URL_MAPPING));

    return links;
  }
}
