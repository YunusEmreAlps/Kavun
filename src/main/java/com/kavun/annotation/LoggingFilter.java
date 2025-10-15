package com.kavun.annotation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import org.slf4j.MDC;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import com.kavun.backend.persistent.repository.ApplicationLogRepository;
import com.kavun.shared.util.MaskPasswordUtils;
import com.kavun.shared.util.core.SecurityUtils;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

  @Autowired
  private ApplicationLogRepository applicationLogRepository;

  @Override
  protected void doFilterInternal(
      @SuppressWarnings("null") @NonNull HttpServletRequest request,
      @SuppressWarnings("null") @NonNull HttpServletResponse response,
      @SuppressWarnings("null") @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    CachedBodyHttpServletRequest cachedRequest = shouldCacheBody(request) ? new CachedBodyHttpServletRequest(request)
        : null;

    HttpServletRequest requestToUse = cachedRequest != null ? cachedRequest : request;

    try {
      String hostname = getHostname();
      String ip = getIp();
      String userIp = getUserIp(request);
      String username = getAuthenticatedUsername(request);
      String user = getAuthenticatedUser(request);
      String url = getUrl(request); // Full URL including query parameters
      String action = getAction(request);
      String queryParams = getQueryParams(request);
      String path = request.getRequestURI(); // Path without query parameters

      MDC.put("hostname", hostname);
      MDC.put("ip", ip);
      MDC.put("userIp", userIp);
      MDC.put("user", username);
      MDC.put("url", url);
      MDC.put("action", action);
      MDC.put("queryParams", queryParams != null ? queryParams : "");

      if (path.startsWith("/api/v1/")) {
        String requestBody = null;
        String oldValues = null;

        if (cachedRequest != null && shouldLogBody(action)) {
          requestBody = cachedRequest.getBody();
        }

        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setLogLevel("INFO");
        applicationLog.setThreadName(Thread.currentThread().getName());
        applicationLog.setLoggerName("com.kavun");
        applicationLog.setLogMessage(buildLogMessage(action, path, oldValues, requestBody));
        applicationLog.setHostname(hostname);
        applicationLog.setIp(ip);
        applicationLog.setLogType("HTTP Request");
        applicationLog.setUserIpAddress(userIp);
        applicationLog.setUsername(user);
        applicationLog.setRequestUrl(url);
        applicationLog.setAction(action);
        applicationLog.setRequestParams(queryParams);

        if (requestBody != null) {
          MDC.put("body", Optional.ofNullable(MaskPasswordUtils.maskPasswordJson(requestBody))
              .map(Object::toString)
              .orElse(""));
        }

        MDC.put("body", Optional.ofNullable(MaskPasswordUtils.maskPasswordJson(requestBody))
            .map(Object::toString)
            .orElse(""));

        // Save the log to the database
        applicationLogRepository.save(applicationLog);
      }

      // Proceed with the filter chain
      filterChain.doFilter(requestToUse, response);
    } finally {
      MDC.put("hostname", getHostname());
      MDC.put("ip", getIp());
      MDC.put("url", getUrl(request));
      MDC.put("action", getAction(requestToUse));
      MDC.put("protocol", request.getProtocol());
      MDC.put("status", String.valueOf(response.getStatus()));
      MDC.put("responseSize",
          response.getHeader("Content-Length") != null ? response.getHeader("Content-Length") : "0");

      LOG.info("");

      MDC.clear();
    }
  }

  // Determine if we should cache the request body
  private boolean shouldCacheBody(HttpServletRequest request) {
    String method = request.getMethod();
    String contentType = request.getContentType();

    return ("POST".equalsIgnoreCase(method) ||
        "PUT".equalsIgnoreCase(method) ||
        "PATCH".equalsIgnoreCase(method)) &&
        contentType != null &&
        contentType.contains("application/json");
  }

  // Determine if we should log the request body based on the HTTP method
  private boolean shouldLogBody(String method) {
    return "POST".equalsIgnoreCase(method) ||
        "PUT".equalsIgnoreCase(method) ||
        "PATCH".equalsIgnoreCase(method);
  }

  // Build the log message based on action, path, old values, and new values
  private String buildLogMessage(String action, String path, String oldValues, String newValues) {
    if (oldValues != null && newValues != null) {
      return String.format("%s request received for %s - Changes: Old[%s] -> New[%s]",
          action, path, truncate(oldValues, 100), truncate(newValues, 100));
    } else if (newValues != null) {
      // if /login or POST with body
      if ("/login".equals(path) || "POST".equalsIgnoreCase(action)) {
        return String.format("%s request received for %s - Data: %s",
            action, path, "[PROTECTED]");
      } else {
        return String.format("%s request received for %s - Data: %s",
            action, path, truncate(newValues, 100));
      }
    } else {
      return String.format("%s request received for %s", action, path);
    }
  }

  // Get the hostname of the server
  private String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  // Get the IP address of the server
  private String getIp() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  // Get the IP address of the user from the request
  private String getUserIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    return (forwardedFor != null && !forwardedFor.isEmpty())
        ? forwardedFor.split(",")[0] // Use the first IP if multiple forwarded IPs are present
        : request.getRemoteAddr();
  }

  // Get the publicId of the authenticated user
  private String getAuthenticatedUser(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {
      // immutable user object (username can be changed but publicId cannot)
      return SecurityUtils.getAuthenticatedUserDetails().getPublicId();
    }
    return "system";
  }

  // Get the username of the authenticated user
  private String getAuthenticatedUsername(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {
      return SecurityUtils.getAuthenticatedUserDetails().getUsername();
    }
    return "system";
  }

  // Get the URL of the request
  private String getUrl(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }

  // Get the query actionType of the request
  private String getAction(HttpServletRequest request) {
    // GET, POST, PUT, DELETE, etc.
    return request.getMethod();
  }

  // Get the query parameters of the request
  private String getQueryParams(HttpServletRequest request) {
    return request.getQueryString();
  }

  //
  private String truncate(String str, int maxLength) {
    if (str == null || str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + "...";
  }
}
