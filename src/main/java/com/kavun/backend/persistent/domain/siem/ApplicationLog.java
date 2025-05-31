package com.kavun.backend.persistent.domain.siem;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "application_logs")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApplicationLog extends BaseEntity<Long> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
}
