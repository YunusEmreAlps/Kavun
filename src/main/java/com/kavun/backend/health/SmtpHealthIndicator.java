package com.kavun.backend.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import com.kavun.config.MailConfig;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.Session;

import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpHealthIndicator implements HealthIndicator {

    private final MailConfig mailConfig;
    private final JavaMailSender mailSender;

    // Health check for SMTP configuration
    @Override
    public Health health() {
        try {
            if (!isConfigurationValid()) {
                return createSecureHealthResult(false, "SMTP Not Configured Properly");
            }

            long startTime = System.currentTimeMillis();

            // Test SMTP connection
            try (SmtpConnection connection = createSmtpConnection()) {
                boolean connected = connection.test();
                long connectionTime = System.currentTimeMillis() - startTime;

                LOG.info("SMTP Health Check {} in {}ms",
                        connected ? "Connected" : "Failed", connectionTime);

                return createSecureHealthResult(connected,
                        connected ? "Connected" : "Connection Failed");

            } catch (Exception e) {
                long connectionTime = System.currentTimeMillis() - startTime;
                LOG.error("SMTP Health Check Failed After {}ms: {}", connectionTime, e.getMessage());
                return createSecureHealthResult(false, "Connection Error");
            }
            
        } catch (Exception e) {
            LOG.error("SMTP Health Check Failed: {}", e.getMessage());
            return createSecureHealthResult(false, "Health Check Failed");
        }
    }

    // Validate SMTP configuration
    private boolean isConfigurationValid() {
        return mailConfig.getHost() != null && !mailConfig.getHost().trim().isEmpty() &&
                mailConfig.getUsername() != null && !mailConfig.getUsername().trim().isEmpty() &&
                mailSender instanceof JavaMailSenderImpl;
    }

    // Create health result
    private Health createSecureHealthResult(boolean isUp, String status) {
        Health.Builder builder = isUp ? Health.up() : Health.down();
        builder.withDetail("status", status);
        return builder.build();
    }

    // Create SMTP connection
    private SmtpConnection createSmtpConnection() {
        return new SmtpConnection((JavaMailSenderImpl) mailSender,
                mailConfig.getHost(), mailConfig.getPort(), mailConfig.getUsername(), mailConfig.getPassword(),
                mailConfig.getProtocol());
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
}
