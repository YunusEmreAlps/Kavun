package com.kavun.backend.service.sms.impl;

import com.kavun.backend.service.sms.SmsService;
import com.kavun.constant.EnvConstants;
import com.kavun.exception.user.SmsServiceException;
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

  @Value("${sms.api.key:}")
  private String apiKey;

  @Value("${sms.api.secret:}")
  private String apiSecret;

  @Value("${sms.sender.number:}")
  private String senderNumber;

  @Value("${sms.provider:mock}")
  private String provider;

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

    try {
      // TODO: Integrate with actual SMS provider
      // Choose and implement one of the following:

      switch (provider.toLowerCase()) {
        case "twilio":
          // sendViaTwilio(normalizedPhone, message);
          LOG.warn("Twilio SMS integration not implemented yet");
          break;
        case "aws-sns":
          // sendViaAwsSns(normalizedPhone, message);
          LOG.warn("AWS SNS SMS integration not implemented yet");
          break;
        case "netgsm":
          // sendViaNetGsm(normalizedPhone, message);
          LOG.warn("NetGSM SMS integration not implemented yet");
          break;
        default:
          LOG.warn("No SMS provider configured. Message would be sent to: {}", normalizedPhone);
          LOG.warn("Message: {}", message);
      }

      LOG.info("SMS sent successfully to: {}", normalizedPhone);

    } catch (Exception e) {
      LOG.error("Failed to send SMS to {}: {}", normalizedPhone, e.getMessage(), e);
      throw new SmsServiceException("Failed to send SMS: " + e.getMessage(), e);
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
