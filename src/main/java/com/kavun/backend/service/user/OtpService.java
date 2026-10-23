package com.kavun.backend.service.user;

import com.kavun.shared.dto.UserDto;
import com.kavun.web.payload.response.OtpChallengeResponse;

/**
 * Sign-in codes: the second step of a login when {@code login.otp.enabled=true}. A code is only
 * ever issued to a user whose password has just been checked, and verifying it only completes that
 * user's sign-in.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public interface OtpService {

  /**
   * Sends a sign-in code to the user through their OTP delivery method. Call it only after the
   * user's password has been checked.
   *
   * @param user the user signing in
   * @return the challenge the client verifies the code against
   * @throws com.kavun.exception.user.OtpValidationException if the user's delivery method cannot
   *     be used (unknown method, or no email/phone to send to)
   */
  OtpChallengeResponse sendSignInCode(UserDto user);

  /**
   * Checks a sign-in code. Each challenge allows a few attempts and can be used once.
   *
   * @param challengeId the challenge id returned by {@link #sendSignInCode}
   * @param code the code the user received
   * @return the username the code was issued to
   * @throws com.kavun.exception.user.OtpValidationException if the challenge is unknown, used,
   *     expired, out of attempts, or the code does not match
   */
  String verifySignInCode(String challengeId, String code);
}
