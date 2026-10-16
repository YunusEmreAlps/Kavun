package com.kavun;

import com.kavun.config.properties.AwsProperties;
import com.kavun.shared.util.core.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.filter.OncePerRequestFilter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * This class provides every bean, and other configurations needed to be used in the testing phase.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class TestConfig {

  /**
   * Test code runs as the platform tenant ({@link PlatformTenantExtension}) and MockMvc executes
   * requests on the test thread, so that setting would leak into request handling. This filter,
   * first in the chain, lets each request resolve its tenant from the signed-in user instead.
   *
   * @return the filter registration
   */
  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> requestTenantIsolation() {
    OncePerRequestFilter filter =
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
            Exception[] failure = new Exception[1];
            TenantContext.runAs(
                null,
                () -> {
                  try {
                    chain.doFilter(request, response);
                  } catch (IOException | ServletException e) {
                    failure[0] = e;
                  }
                });
            if (failure[0] instanceof IOException e) {
              throw e;
            }
            if (failure[0] instanceof ServletException e) {
              throw e;
            }
          }
        };
    var registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Value("${spring.mail.username}")
  private transient String mailUsername;

  @Value("${spring.mail.password}")
  private transient String mailPassword;

  @Value("${spring.mail.host}")
  private transient String mailHost;

  @Value("${spring.mail.port}")
  private transient int mailPort;

  @Value("${spring.mail.protocol}")
  private transient String mailProtocol;

  /**
   * Creates a JavaMailSender bean.
   *
   * @return javaMailSender
   */
  @Bean
  public JavaMailSender mailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(mailHost);
    mailSender.setPort(mailPort);
    mailSender.setUsername(mailUsername);
    mailSender.setPassword(mailPassword);
    mailSender.setProtocol(mailProtocol);

    return mailSender;
  }

  @Bean
  public GreenMail greenMail() {
    ServerSetup smtp = ServerSetupTest.SMTP;
    smtp.setServerStartupTimeout(5000);

    return new GreenMail(smtp)
        .withConfiguration(GreenMailConfiguration.aConfig().withUser(mailUsername, mailPassword));
  }

  /**
   * A bean to be used by AmazonS3 Service.
   *
   * @param props the aws properties
   * @return instance of S3Client
   */
  @Bean
  public S3Client s3Client(AwsProperties props) {
    // Create the credential provider
    var credentials =
        AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey());

    return S3Client.builder()
        .region(Region.of(props.getRegion()))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();
  }
}
