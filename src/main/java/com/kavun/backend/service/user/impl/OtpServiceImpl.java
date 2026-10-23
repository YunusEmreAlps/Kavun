package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Otp;
import com.kavun.backend.persistent.repository.OtpRepository;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.sms.SmsService;
import com.kavun.backend.service.user.OtpService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.EnvConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.enums.OtpDeliveryMethod;
import com.kavun.exception.user.OtpValidationException;
import com.kavun.shared.dto.UserDto;
import com.kavun.web.payload.response.OtpChallengeResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The OtpServiceImpl class is an implementation for the OtpService Interface.
 *
 * <p>A code is stored with the username it was issued to, not the address it was sent to, and is
 * looked up by its random public id, so a code can only ever complete the sign-in it was sent for.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OtpServiceImpl implements OtpService {

  private static final int VISIBLE_PHONE_DIGITS = 4;
  private static final String TEST_OTP_CODE = "app.test-otp-code";

  private final transient Environment environment;
  private final transient OtpRepository otpRepository;
  private final transient EmailService emailService;
  private final transient SmsService smsService;

  @Override
  @Transactional
  public OtpChallengeResponse sendSignInCode(UserDto user) {
    OtpDeliveryMethod method =
        EnumUtils.getEnum(OtpDeliveryMethod.class, user.getOtpDeliveryMethod());
    if (method == null) {
      throw new OtpValidationException(
          StringUtils.isBlank(user.getOtpDeliveryMethod())
              ? AuthConstants.BLANK_OTP_DELIVERY_METHOD
              : AuthConstants.INVALID_OTP_DELIVERY_METHOD);
    }

    boolean sms = method == OtpDeliveryMethod.SMS;
    String destination = sms ? user.getPhone() : user.getEmail();
    if (StringUtils.isBlank(destination)) {
      throw new OtpValidationException(
          sms ? AuthConstants.USER_HAS_NO_PHONE_FOR_OTP : AuthConstants.USER_HAS_NO_EMAIL_FOR_OTP);
    }

    // Development and test have no mail server or SMS provider: they use the fixed code
    // app.test-otp-code and send nothing.
    boolean devOrTest =
        environment.acceptsProfiles(Profiles.of(EnvConstants.DEVELOPMENT, EnvConstants.TEST));

    Otp otp = new Otp();
    otp.setTarget(user.getUsername());
    otp.setCode(devOrTest ? environment.getRequiredProperty(TEST_OTP_CODE) : Otp.generateOtp());
    otpRepository.save(otp);

    if (devOrTest) {
      LOG.info("Sign-in code for {} not sent; use the test code {}", user.getUsername(), otp.getCode());
    } else if (sms) {
      smsService.sendOtpSms(destination, otp.getCode());
    } else {
      emailService.sendOtpEmail(destination, otp.getCode(), user.getFirstName());
    }

    return new OtpChallengeResponse(otp.getPublicId(), mask(destination, sms));
  }

  // A failed attempt is counted even though the method throws, so the transaction must commit.
  @Override
  @Transactional(noRollbackFor = OtpValidationException.class)
  public String verifySignInCode(String challengeId, String code) {
    Otp otp =
        otpRepository
            .findByPublicId(challengeId)
            .orElseThrow(() -> new OtpValidationException(AuthConstants.OTP_NOT_FOUND));

    if (otp.isValid() && otp.getCode().equals(code)) {
      otp.setUsedAt(LocalDateTime.now());
      otp.setActive(false);
      return otp.getTarget();
    }

    otp.setFailedAttempts(otp.getFailedAttempts() + 1);
    String errorMessage;
    if (otp.isExpired()) {
      otp.setActive(false);
      errorMessage = AuthConstants.OTP_EXPIRED;
    } else if (otp.getFailedAttempts() >= SecurityConstants.OTP_MAX_ATTEMPTS) {
      otp.setActive(false);
      errorMessage = AuthConstants.OTP_MAX_ATTEMPTS;
    } else {
      errorMessage = AuthConstants.OTP_NOT_VERIFIED;
    }
    LOG.warn("Sign-in code rejected for {}: {}", otp.getTarget(), errorMessage);
    throw new OtpValidationException(errorMessage);
  }

  // Shows enough for the user to recognise the address without disclosing it.
  private static String mask(String destination, boolean phone) {
    if (phone) {
      int visible = destination.length() > VISIBLE_PHONE_DIGITS ? VISIBLE_PHONE_DIGITS : 0;
      int hidden = destination.length() - visible;
      return "*".repeat(hidden) + destination.substring(hidden);
    }
    int at = destination.indexOf('@');
    return at > 0 ? destination.charAt(0) + "***" + destination.substring(at) : "***";
  }
}
