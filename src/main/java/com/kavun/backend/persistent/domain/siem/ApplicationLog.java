package com.kavun.backend.persistent.domain.siem;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity for storing application logs for SIEM integration and audit trail.
 * Captures HTTP request metadata, user context, and timing information.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "application_logs", indexes = {
    @Index(name = "idx_app_log_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_app_log_username", columnList = "username"),
    @Index(name = "idx_app_log_created_at", columnList = "created_at")
})
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApplicationLog extends BaseEntity<Long> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "log_level", nullable = false, length = 50)
    private String logLevel;

    @Column(name = "thread_name", nullable = false, length = 100)
    private String threadName;

    @Column(name = "logger_name", nullable = false, length = 255)
    private String loggerName;

    @Column(name = "log_message", columnDefinition = "text")
    private String logMessage;

    @Column(name = "hostname", length = 255)
    private String hostname;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "log_type", length = 100)
    private String logType;

    @Column(name = "user_ip_address", length = 45)
    private String userIpAddress;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "request_url", length = 2048)
    private String requestUrl;

    @Column(name = "action", length = 255)
    private String action;

    @Column(name = "request_params", columnDefinition = "text")
    private String requestParams;

    @Column(name = "state_before", columnDefinition = "text")
    private String stateBefore;

    @Column(name = "state_after", columnDefinition = "text")
    private String stateAfter;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "request_body", columnDefinition = "text")
    private String requestBody;
}
