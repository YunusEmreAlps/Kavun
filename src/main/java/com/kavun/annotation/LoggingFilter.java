package com.kavun.annotation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.MDC;
import lombok.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import com.kavun.backend.persistent.repository.ApplicationLogRepository;

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

    try {
      String hostname = getHostname();
      String ip = getIp();
      String userIp = getUserIp(request);
      String user = getAuthenticatedUser(request);
      String url = getUrl(request); // Full URL including query parameters
      String action = getAction(request);
      String queryParams = getQueryParams(request);
      String path = request.getRequestURI(); // Path without query parameters

      MDC.put("hostname", hostname);
      MDC.put("ip", ip);
      MDC.put("userIp", userIp);
      MDC.put("user", user);
      MDC.put("url", url);
      MDC.put("action", action);
      MDC.put("queryParams", queryParams != null ? queryParams : "");

      if (path.startsWith("/api/v1/")) {
        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setLogLevel("INFO");
        applicationLog.setThreadName(Thread.currentThread().getName());
        applicationLog.setLoggerName("com.kavun");
        applicationLog.setLogMessage("Request received");
        applicationLog.setHostname(hostname);
        applicationLog.setIp(ip);
        applicationLog.setLogType("HTTP Request");
        applicationLog.setUserIpAddress(userIp);
        applicationLog.setUsername(user);
        applicationLog.setRequestUrl(url);
        applicationLog.setAction(action);
        applicationLog.setRequestParams(queryParams);
        // Save the log to the database
        applicationLogRepository.save(applicationLog);
      }

      // Proceed with the filter chain
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear(); // Ensure MDC is cleared after the request is processed
    }
  }

  /**
   * Get the hostname of the server
   *
   * @return the hostname
   */
  private String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  /**
   * Get the IP address of the server
   *
   * @return the IP address
   */
  private String getIp() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  /**
   * Get the IP address of the user
   *
   * @param request
   * @return the user's IP address
   */
  private String getUserIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    return (forwardedFor != null && !forwardedFor.isEmpty())
        ? forwardedFor.split(",")[0] // Use the first IP if multiple forwarded IPs are present
        : request.getRemoteAddr();
  }

  /**
   * Get the authenticated user
   *
   * @param request
   * @return the authenticated user
   */
  private String getAuthenticatedUser(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {
      return authentication.getName();
    }
    return "system"; // unauthenticated user
  }

  /**
   * Get the URL of the request
   *
   * @param request
   * @return the URL
   */
  private String getUrl(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }

  /**
   * Get the query actionType of the request
   *
   * @param request
   * @return the query actionType
   */
  private String getAction(HttpServletRequest request) {
    // GET, POST, PUT, DELETE, etc.
    return request.getMethod();
  }

  /**
   * Get the query parameters of the request
   *
   * @param request
   * @return the query parameters
   */
  private String getQueryParams(HttpServletRequest request) {
    return request.getQueryString();
  }
}
