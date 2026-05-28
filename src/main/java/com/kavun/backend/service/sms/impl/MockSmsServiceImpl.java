package com.kavun.backend.service.sms.impl;

import com.kavun.backend.service.sms.SmsService;
import com.kavun.exception.user.SmsServiceException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of SMS service for development and testing.
 * Logs SMS messages instead of actually sending them.
 * Replace this with real SMS provider implementation (Twilio, AWS SNS, NetGSM, etc.) for production.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Profile({"development", "test"})
public class MockSmsServiceImpl implements SmsService {

  // Turkish phone number pattern: +90XXXXXXXXXX or 05XXXXXXXXX
  private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+90|0)?[5][0-9]{9}$");

  @Override
  public void sendSms(String phoneNumber, String message) {
    LOG.info("=== MOCK SMS SERVICE ===");
    LOG.info("To: {}", phoneNumber);
    LOG.info("Message: {}", message);
    LOG.info("========================");

    if (!isValidPhoneNumber(phoneNumber)) {
      throw new SmsServiceException("Invalid phone number format: " + phoneNumber);
    }

    if (StringUtils.isBlank(message)) {
      throw new SmsServiceException("SMS message cannot be empty");
    }

    // In dev/test, just log - don't actually send
    LOG.info("SMS would be sent to {} in production environment", phoneNumber);
  }

  @Override
  public void sendOtpSms(String phoneNumber, String otpCode) {
    String message = String.format("Your OTP code is: %s. Valid for 5 minutes. Do not share this code.", otpCode);
    sendSms(phoneNumber, message);
  }

  @Override
  public boolean isValidPhoneNumber(String phoneNumber) {
    if (StringUtils.isBlank(phoneNumber)) {
      return false;
    }
    return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
  }
}
