package com.kavun.backend.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.Session;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpHealthIndicator implements HealthIndicator {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.protocol:smtp}")
    private String mailProtocol;

    @Value("${management.health.mail.show-details:false}")
    private boolean showDetails;

    @Value("${management.health.mail.cache-duration:30}")
    private int cacheDurationSeconds;

    // Cache the health result to avoid frequent connection attempts
    private final AtomicReference<CachedHealthResult> cachedResult = new AtomicReference<>();

    @Override
    public Health health() {
        // Check cached result first
        CachedHealthResult cached = cachedResult.get();
        if (cached != null && cached.isValid(cacheDurationSeconds)) {
            LOG.debug("Returning cached SMTP health result");
            return cached.health;
        }

        // Perform actual health check
        Health health = performHealthCheck();

        // Cache the result
        cachedResult.set(new CachedHealthResult(health, LocalDateTime.now()));

        return health;
    }

    private Health performHealthCheck() {
        if (!isConfigurationValid()) {
            return createSecureHealthResult(false, "SMTP not configured");
        }

        long startTime = System.currentTimeMillis();

        try (SmtpConnection connection = createSmtpConnection()) {
            boolean connected = connection.test();
            long connectionTime = System.currentTimeMillis() - startTime;

            LOG.info("SMTP health check {} in {}ms",
                    connected ? "succeeded" : "failed", connectionTime);

            return createSecureHealthResult(connected,
                    connected ? "Connected" : "Connection failed");

        } catch (Exception e) {
            long connectionTime = System.currentTimeMillis() - startTime;

            LOG.error("SMTP health check failed after {}ms: {}", connectionTime, e.getMessage());

            return createSecureHealthResult(false, "Connection error");
        }
    }

    private boolean isConfigurationValid() {
        return mailHost != null && !mailHost.trim().isEmpty() &&
                mailUsername != null && !mailUsername.trim().isEmpty() &&
                mailSender instanceof JavaMailSenderImpl;
    }

    private Health createSecureHealthResult(boolean isUp, String status) {
        Health.Builder builder = isUp ? Health.up() : Health.down();

        if (showDetails) {
            builder.withDetail("smtp_status", status)
                    .withDetail("host", maskSensitiveInfo(mailHost))
                    .withDetail("port", mailPort)
                    .withDetail("username", maskSensitiveInfo(mailUsername))
                    .withDetail("protocol", mailProtocol);
        } else {
            // Only show basic status for security
            builder.withDetail("status", status);
        }

        return builder.build();
    }

    private String maskSensitiveInfo(String info) {
        if (info == null || info.length() <= 4)
            return "***";
        return info.substring(0, 2) + "***" + info.substring(info.length() - 2);
    }

    private SmtpConnection createSmtpConnection() {
        return new SmtpConnection((JavaMailSenderImpl) mailSender,
                mailHost, mailPort, mailUsername, mailPassword, mailProtocol);
    }

    // Inner class for managing SMTP connections
    private static class SmtpConnection implements AutoCloseable {
        private final JavaMailSenderImpl mailSender;
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String protocol;
        private Transport transport;

        public SmtpConnection(JavaMailSenderImpl mailSender, String host, int port,
                String username, String password, String protocol) {
            this.mailSender = mailSender;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.protocol = protocol;
        }

        public boolean test() throws MessagingException {
            Session session = mailSender.getSession();
            configureSSLTrust(session.getProperties(), host);

            transport = session.getTransport(protocol);
            transport.connect(host, port, username, password);

            return transport.isConnected();
        }

        private void configureSSLTrust(Properties props, String host) {
            // Secure SSL configuration
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.ssl.checkserveridentity", "false");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

            // Connection timeouts for better performance
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
        }

        @Override
        public void close() {
            if (transport != null && transport.isConnected()) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    LOG.warn("Error closing SMTP transport: {}", e.getMessage());
                }
            }
        }
    }

    // Inner class for caching health results
    private static class CachedHealthResult {
        private final Health health;
        private final LocalDateTime timestamp;

        public CachedHealthResult(Health health, LocalDateTime timestamp) {
            this.health = health;
            this.timestamp = timestamp;
        }

        public boolean isValid(int cacheDurationSeconds) {
            return Duration.between(timestamp, LocalDateTime.now())
                    .getSeconds() < cacheDurationSeconds;
        }
    }
}
