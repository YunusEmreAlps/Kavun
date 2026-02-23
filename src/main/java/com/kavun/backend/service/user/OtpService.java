package com.kavun.backend.service.user;

import java.util.Map;

/**
 * This OtpService interface is the contract for the otp service operations.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public interface OtpService {
  // Generates the otp code for the user with the given email or sms.
  public Map<String, Object> generateOtp(String target);

  // Validates the otp code for the user with the given email or sms.
  public Boolean validateOtp(Long id, String target, String otp);

  // Generates OTP and sends it via SMS using CMP.
  public Map<String, Object> generateAndSendOtpSms(String phoneNumber);

  // Generates OTP and sends it via Email.
  public Map<String, Object> generateAndSendOtpEmail(String email);
}
