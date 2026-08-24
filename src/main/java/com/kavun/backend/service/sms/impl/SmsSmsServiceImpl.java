package com.kavun.backend.service.sms.impl;

import com.kavun.backend.service.sms.SmsService;
import com.kavun.constant.EnvConstants;
import com.kavun.exception.user.SmsServiceException;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Production-ready SMS service implementation.
 * Currently configured for Turkish phone numbers and ready for SMS provider integration.
 *
 * <p>Supported SMS Providers (choose one and configure):
 * <ul>
 *   <li>Twilio - https://www.twilio.com/docs/sms</li>
 *   <li>AWS SNS - https://docs.aws.amazon.com/sns/latest/dg/sns-mobile-phone-number-as-subscriber.html</li>
 *   <li>NetGSM (Turkey) - https://www.netgsm.com.tr/dokuman/</li>
 *   <li>İyziSMS (Turkey) - https://www.iyzico.com/</li>
 * </ul>
 *
 * <p>Configuration properties (add to application-production.properties):
 * <pre>
 * sms.provider=twilio|aws-sns|netgsm
 * sms.api.key=your-api-key
 * sms.api.secret=your-api-secret
 * sms.sender.number=+905551234567
 * </pre>
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Profile({EnvConstants.PRODUCTION, EnvConstants.DOCKER})
public class SmsSmsServiceImpl implements SmsService {

  // Turkish phone number pattern: +90XXXXXXXXXX or 05XXXXXXXXX
  private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+90|0)?[5][0-9]{9}$");

  // None of these providers have a real integration wired up yet (see the
  // commented-out sendVia*() methods below) - until one is implemented,
  // sendSms() must fail loudly instead of silently "succeeding".
  private static final Set<String> UNIMPLEMENTED_PROVIDERS = Set.of("twilio", "aws-sns", "netgsm");

  @Value("${sms.api.key:}")
  private String apiKey;

  @Value("${sms.api.secret:}")
  private String apiSecret;

  @Value("${sms.sender.number:}")
  private String senderNumber;

  @Value("${sms.provider:mock}")
  private String provider;

  /**
   * Warn loudly at startup if this bean is active (production/docker) without a real
   * SMS provider wired up, so the gap is visible in logs before any OTP is ever sent -
   * not discovered by a user who never received their code.
   */
  @PostConstruct
  private void warnIfNoProviderConfigured() {
    if (!UNIMPLEMENTED_PROVIDERS.contains(provider.toLowerCase())) {
      LOG.warn("SmsSmsServiceImpl is active but 'sms.provider={}' has no real integration implemented. "
          + "Any call to sendSms()/sendOtpSms() will throw SmsServiceException until a provider "
          + "(twilio, aws-sns, netgsm) is implemented and configured. "
          + "Do not enable login.otp.enabled with SMS delivery until this is resolved.", provider);
    }
  }

  @Override
  public void sendSms(String phoneNumber, String message) {
    LOG.info("Attempting to send SMS to: {}", phoneNumber);

    // Validate inputs
    if (!isValidPhoneNumber(phoneNumber)) {
      LOG.error("Invalid phone number format: {}", phoneNumber);
      throw new SmsServiceException("Invalid phone number format: " + phoneNumber);
    }

    if (StringUtils.isBlank(message)) {
      LOG.error("SMS message cannot be empty");
      throw new SmsServiceException("SMS message cannot be empty");
    }

    // Normalize phone number to E.164 format (+90XXXXXXXXXX)
    String normalizedPhone = normalizePhoneNumber(phoneNumber);

    // Fail fast: none of the supported providers are actually integrated yet
    // (see the commented-out sendVia*() methods below). Returning normally here
    // without sending anything would make the caller (and the end user waiting
    // on an OTP) believe delivery succeeded when it silently did not.
    switch (provider.toLowerCase()) {
      case "twilio":
        // sendViaTwilio(normalizedPhone, message);
        throw new SmsServiceException(
            "SMS provider 'twilio' is not yet integrated. Implement sendViaTwilio() in "
                + "SmsSmsServiceImpl before sending to " + normalizedPhone + ".");
      case "aws-sns":
        // sendViaAwsSns(normalizedPhone, message);
        throw new SmsServiceException(
            "SMS provider 'aws-sns' is not yet integrated. Implement sendViaAwsSns() in "
                + "SmsSmsServiceImpl before sending to " + normalizedPhone + ".");
      case "netgsm":
        // sendViaNetGsm(normalizedPhone, message);
        throw new SmsServiceException(
            "SMS provider 'netgsm' is not yet integrated. Implement sendViaNetGsm() in "
                + "SmsSmsServiceImpl before sending to " + normalizedPhone + ".");
      default:
        throw new SmsServiceException(
            "No SMS provider configured (sms.provider=" + provider + "). Set 'sms.provider' to a "
                + "supported value and implement the corresponding integration before sending to "
                + normalizedPhone + ".");
    }
  }

  @Override
  public void sendOtpSms(String phoneNumber, String otpCode) {
    String message = String.format(
        "Your OTP code is: %s. Valid for 5 minutes. Do not share this code with anyone.",
        otpCode
    );
    sendSms(phoneNumber, message);
  }

  @Override
  public boolean isValidPhoneNumber(String phoneNumber) {
    if (StringUtils.isBlank(phoneNumber)) {
      return false;
    }
    return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
  }

  /**
   * Normalizes phone number to E.164 format (+90XXXXXXXXXX).
   *
   * @param phoneNumber the phone number to normalize
   * @return normalized phone number in E.164 format
   */
  private String normalizePhoneNumber(String phoneNumber) {
    String cleaned = phoneNumber.trim();

    // Already in E.164 format
    if (cleaned.startsWith("+90")) {
      return cleaned;
    }

    // Convert 05XXXXXXXXX to +905XXXXXXXXX
    if (cleaned.startsWith("0")) {
      return "+9" + cleaned;
    }

    // Assume 5XXXXXXXXX, add +90
    if (cleaned.startsWith("5") && cleaned.length() == 10) {
      return "+90" + cleaned;
    }

    return cleaned;
  }

  // ============================================================================
  // SMS Provider Integration Methods (Implement as needed)
  // ============================================================================

  /**
   * TODO: Implement Twilio SMS integration.
   * Add dependency: implementation 'com.twilio.sdk:twilio:9.+'
   */
  // private void sendViaTwilio(String phoneNumber, String message) {
  //   Twilio.init(apiKey, apiSecret);
  //   Message.creator(
  //       new PhoneNumber(phoneNumber),
  //       new PhoneNumber(senderNumber),
  //       message
  //   ).create();
  // }

  /**
   * TODO: Implement AWS SNS SMS integration.
   * Add dependency: implementation 'software.amazon.awssdk:sns:2.+'
   */
  // private void sendViaAwsSns(String phoneNumber, String message) {
  //   SnsClient snsClient = SnsClient.builder()
  //       .region(Region.EU_CENTRAL_1)
  //       .credentialsProvider(DefaultCredentialsProvider.create())
  //       .build();
  //
  //   PublishRequest request = PublishRequest.builder()
  //       .message(message)
  //       .phoneNumber(phoneNumber)
  //       .build();
  //
  //   snsClient.publish(request);
  // }

  /**
   * TODO: Implement NetGSM (Turkey) SMS integration.
   * Documentation: https://www.netgsm.com.tr/dokuman/
   */
  // private void sendViaNetGsm(String phoneNumber, String message) {
  //   // Implement NetGSM API call
  //   // Use RestTemplate or WebClient to call NetGSM HTTP API
  // }
}
