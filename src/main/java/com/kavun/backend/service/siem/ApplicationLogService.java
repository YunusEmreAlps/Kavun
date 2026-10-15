package com.kavun.backend.service.siem;

import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.context.HttpLogContext;
import com.kavun.backend.persistent.context.MethodLogContext;
import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import com.kavun.backend.persistent.repository.ApplicationLogRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.ApplicationLogDto;
import com.kavun.shared.dto.mapper.ApplicationLogMapper;
import com.kavun.shared.request.ApplicationLogRequest;
import com.kavun.shared.util.core.NetworkUtils;

import lombok.extern.slf4j.Slf4j;
import static com.kavun.constant.LoggingConstants.*;

/**
 * Application log service to provide implementation for the definitions about
 * an application log.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ApplicationLogService extends
        AbstractService<ApplicationLogRequest, ApplicationLog, ApplicationLogDto, ApplicationLogRepository, ApplicationLogMapper> {

  private static final String CACHED_HOSTNAME = NetworkUtils.resolveHostname();
  private static final String CACHED_IP = NetworkUtils.resolveIp();

    public ApplicationLogService(ApplicationLogMapper mapper, ApplicationLogRepository repository) {
        super(mapper, repository);
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of(
            "correlationId", "logLevel", "threadName", "loggerName", "logMessage",
            "hostname", "ip", "logType", "userIpAddress", "username", "requestUrl", "action",
            "requestParams", "httpStatus", "deviceId", "deviceType", "operatingSystem",
            "browser", "createdAt");
    }

  @Async("taskExecutor")
  public void persistHttpLog(HttpLogContext ctx) {
    try {
      ApplicationLog log = ApplicationLog.builder()
          .correlationId(ctx.correlationId())
          .logLevel(determineLogLevel(ctx.httpStatus()))
          .logType(LOG_TYPE_HTTP_REQUEST)
          .loggerName(ctx.loggerName())
          .logMessage(ctx.logMessage())
          .threadName(Thread.currentThread().getName())
          .hostname(CACHED_HOSTNAME)
          .ip(CACHED_IP)
          .userIpAddress(ctx.userIp())
          .username(ctx.username())
          .userId(ctx.userId())
          .requestUrl(fit(ctx.requestUrl(), MAX_REQUEST_URL_LENGTH))
          .action(ctx.action())
          .requestParams(ctx.requestParams())
          .requestBody(ctx.requestBody())
          .durationMs(ctx.durationMs())
          .httpStatus(ctx.httpStatus())
          .deviceId(fit(ctx.deviceId(), MAX_DEVICE_ID_LENGTH))
          .deviceType(ctx.deviceType())
          .operatingSystem(ctx.operatingSystem())
          .browser(ctx.browser())
          .userAgent(ctx.userAgent())
          .stateBefore(ctx.stateBefore())
          .stateAfter(ctx.stateAfter())
          .stateDiff(ctx.stateDiff())
          .build();

      save(log);
    } catch (Exception e) {
      // Handle exception or log error
      LOG.error("Failed to persist HTTP log: {}", e.getMessage());
    }
  }

  @Async("taskExecutor")
  public void persistMethodLog(MethodLogContext ctx) {
    try {
      ApplicationLog log = ApplicationLog.builder()
          .correlationId(UUID.randomUUID().toString())
          .logLevel("INFO")
          .logType(ctx.logType()) // SCHEDULER, ASYNC, EVENT vb.
          .loggerName(ctx.method())
          .logMessage("Executed: " + ctx.method())
          .threadName(Thread.currentThread().getName())
          .hostname(CACHED_HOSTNAME)
          .ip(CACHED_IP)
          .durationMs(ctx.durationMs())
          .stateBefore(ctx.stateBefore())
          .stateAfter(ctx.stateAfter())
          .stateDiff(ctx.stateDiff())
          .build();

      save(log);
    } catch (Exception e) {
      // Handle exception or log error
      LOG.error("Failed to persist method log: {}", e.getMessage());
    }
  }

  private void save(ApplicationLog applicationLog) {
    try {
      this.repository.save(applicationLog);
    } catch (Exception e) {
      LOG.error("Failed to save application log: {}", e.getMessage());
    }
  }

  private static String fit(String value, int maxLength) {
    return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String determineLogLevel(int status) {
    if (status >= 500)
      return LOG_LEVEL_ERROR;
    if (status >= 400)
      return LOG_LEVEL_WARN;
    return LOG_LEVEL_INFO;
  }
}
