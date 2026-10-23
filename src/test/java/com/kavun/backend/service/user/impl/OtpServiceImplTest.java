package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Otp;
import com.kavun.backend.persistent.repository.OtpRepository;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.sms.SmsService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.EnvConstants;
import com.kavun.enums.OtpDeliveryMethod;
import com.kavun.exception.user.OtpValidationException;
import com.kavun.shared.dto.UserDto;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

  private static final String USERNAME = "jane";
  private static final String EMAIL = "jane.doe@example.com";
  private static final String PHONE = "+905551234567";
  private static final String CODE = "123456";

  @Mock private transient OtpRepository otpRepository;
  @Mock private transient EmailService emailService;
  @Mock private transient SmsService smsService;

  private transient MockEnvironment environment;
  private transient OtpServiceImpl otpService;
  private transient UserDto user;

  @BeforeEach
  void setUp() {
    environment = new MockEnvironment();
    otpService = new OtpServiceImpl(environment, otpRepository, emailService, smsService);
    user = new UserDto();
    user.setUsername(USERNAME);
    user.setFirstName("Jane");
    user.setEmail(EMAIL);
    user.setPhone(PHONE);
  }

  @Test
  void codeIsIssuedToTheUserAndEmailedToTheirMaskedAddress() {
    var challenge = otpService.sendSignInCode(user);

    var otp = savedOtp();
    Assertions.assertEquals(USERNAME, otp.getTarget());
    Assertions.assertTrue(otp.getCode().matches("\\d{6}"));
    Mockito.verify(emailService).sendOtpEmail(EMAIL, otp.getCode(), "Jane");
    Mockito.verifyNoInteractions(smsService);
    Assertions.assertEquals(otp.getPublicId(), challenge.id());
    Assertions.assertEquals("j***@example.com", challenge.target());
  }

  @Test
  void smsCodeGoesToThePhoneAndShowsOnlyItsLastDigits() {
    user.setOtpDeliveryMethod(OtpDeliveryMethod.SMS.name());

    var challenge = otpService.sendSignInCode(user);

    var code = savedOtp().getCode();
    Mockito.verify(smsService).sendOtpSms(PHONE, code);
    Mockito.verifyNoInteractions(emailService);
    Assertions.assertEquals("*********4567", challenge.target());
  }

  @Test
  void smsDeliveryWithoutAPhoneIsRejected() {
    user.setOtpDeliveryMethod(OtpDeliveryMethod.SMS.name());
    user.setPhone(null);

    var e = Assertions.assertThrows(
        OtpValidationException.class, () -> otpService.sendSignInCode(user));

    Assertions.assertEquals(AuthConstants.USER_HAS_NO_PHONE_FOR_OTP, e.getMessage());
    Mockito.verifyNoInteractions(otpRepository, smsService);
  }

  @Test
  void developmentUsesTheFixedTestCodeAndSendsNothing() {
    environment.setActiveProfiles(EnvConstants.DEVELOPMENT);
    environment.setProperty("app.test-otp-code", "190303");

    otpService.sendSignInCode(user);

    Assertions.assertEquals("190303", savedOtp().getCode());
    Mockito.verifyNoInteractions(emailService, smsService);
  }

  @Test
  void rightCodeReturnsItsUserAndCannotBeUsedAgain() {
    var otp = pendingOtp();

    Assertions.assertEquals(USERNAME, otpService.verifySignInCode(otp.getPublicId(), CODE));

    Assertions.assertFalse(otp.getActive());
    Assertions.assertNotNull(otp.getUsedAt());
    Assertions.assertThrows(
        OtpValidationException.class, () -> otpService.verifySignInCode(otp.getPublicId(), CODE));
  }

  @Test
  void thirdWrongCodeEndsTheChallenge() {
    var otp = pendingOtp();

    Assertions.assertEquals(AuthConstants.OTP_NOT_VERIFIED, rejection(otp, "000000"));
    Assertions.assertEquals(AuthConstants.OTP_NOT_VERIFIED, rejection(otp, "000001"));
    Assertions.assertEquals(AuthConstants.OTP_MAX_ATTEMPTS, rejection(otp, "000002"));

    Assertions.assertFalse(otp.getActive());
    Assertions.assertEquals(AuthConstants.OTP_MAX_ATTEMPTS, rejection(otp, CODE));
  }

  @Test
  void unknownChallengeIsRejected() {
    Mockito.when(otpRepository.findByPublicId("unknown")).thenReturn(Optional.empty());

    var e = Assertions.assertThrows(
        OtpValidationException.class, () -> otpService.verifySignInCode("unknown", CODE));

    Assertions.assertEquals(AuthConstants.OTP_NOT_FOUND, e.getMessage());
  }

  private Otp savedOtp() {
    var saved = ArgumentCaptor.forClass(Otp.class);
    Mockito.verify(otpRepository).save(saved.capture());
    return saved.getValue();
  }

  private Otp pendingOtp() {
    var otp = new Otp();
    otp.setTarget(USERNAME);
    otp.setCode(CODE);
    Mockito.when(otpRepository.findByPublicId(otp.getPublicId())).thenReturn(Optional.of(otp));
    return otp;
  }

  private String rejection(Otp otp, String code) {
    return Assertions.assertThrows(
            OtpValidationException.class,
            () -> otpService.verifySignInCode(otp.getPublicId(), code))
        .getMessage();
  }
}
