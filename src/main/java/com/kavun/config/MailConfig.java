package com.kavun.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Getter
@Configuration
@Slf4j
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.protocol:smtp}")
    private String protocol;

    // SSL Configuration
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable}")
    private String sslEnable;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.port}")
    private String socketFactoryPort;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.class}")
    private String socketFactoryClass;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.fallback}")
    private String socketFactoryFallback;

    // SSL Trust Configuration
    @Value("${spring.mail.properties.mail.smtp.ssl.trust}")
    private String sslTrust;

    @Value("${spring.mail.properties.mail.smtp.ssl.checkserveridentity}")
    private String sslCheckServerIdentity;

    @Value("${spring.mail.properties.mail.smtp.ssl.protocols}")
    private String sslProtocols;

    // TLS
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String starttlsEnable;

    // Connection timeouts
    @Value("${spring.mail.properties.mail.smtp.connectiontimeout}")
    private String connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout}")
    private String timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout}")
    private String writeTimeout;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        logConfiguration();

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol(protocol);

        Properties props = mailSender.getJavaMailProperties();

        // SSL Configuration
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.ssl.enable", sslEnable);
        props.put("mail.smtp.socketFactory.port", socketFactoryPort);
        props.put("mail.smtp.socketFactory.class", socketFactoryClass);
        props.put("mail.smtp.socketFactory.fallback", socketFactoryFallback);

        // SSL Trust Configuration
        props.put("mail.smtp.ssl.trust", sslTrust);
        props.put("mail.smtp.ssl.checkserveridentity", sslCheckServerIdentity);
        props.put("mail.smtp.ssl.protocols", sslProtocols);

        // TLS Configuration
        props.put("mail.smtp.starttls.enable", starttlsEnable);

        // Connection timeouts
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);

        LOG.info("Mail sender configured successfully with SSL on port {}", port);
        return mailSender;
    }

    private void logConfiguration() {
        LOG.info("-".repeat(32));
        LOG.info("CONFIGURING MAIL SENDER");
        LOG.info("Host: {}", host);
        LOG.info("Port: {}", port);
        LOG.info("Username: {}", username);
        LOG.info("Protocol: {}", protocol);
        LOG.info("-".repeat(32));
    }
}
