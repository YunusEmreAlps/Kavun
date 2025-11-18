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
  /**
   * Generates the otp code for the user with the given email or sms.
   *
   * @param target the target to email or sms
   * @return Map containing publicId and target
   */
  public Map<String, Object> generateOtp(String target);

  /**
   * Validates the otp code for the user with the given email or sms.
   *
   * @param publicId the public id of the otp
   * @param target the target to email or sms
   * @param otp the otp code to validate
   * @return true if the otp code is valid, false otherwise
   */
  public Boolean validateOtp(String publicId, String target, String otp);
}
