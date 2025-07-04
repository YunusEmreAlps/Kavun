package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Otp;
import com.kavun.backend.persistent.repository.OtpRepository;
import com.kavun.backend.service.user.OtpService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.web.payload.response.CustomResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The OtpServiceImpl class is an implementation for the OtpService Interface.
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

    private final transient OtpRepository otpRepository;

    /**
     * Generates the otp code for the user with the given email or sms.
     *
     * @param target the target to email or sms
     * @return the generated otp code
     */
    @Override
    @Transactional
    public CustomResponse<Object> generateOtp(String target) {
        Otp otpEntity = new Otp();

        otpEntity.setTarget(target);
        otpEntity.setCode(Otp.generateOtp());
        otpEntity.setActive(true);
        otpRepository.save(otpEntity);

        Map<String, Object> params = new HashMap<>();
        params.put("publicId", otpEntity.getPublicId());
        params.put("target", otpEntity.getTarget());

        return CustomResponse.of(
                HttpStatus.OK, params, AuthConstants.OTP_GENERATED, SecurityConstants.GENERATE_OTP);
    }

    /**
     * Validates the otp code for the user with the given email or sms.
     *
     * @param publicId the public id of the otp
     * @param target   the target to email or sms
     * @param code     the otp code to validate
     * @return true if the otp code is valid, false otherwise
     */
    @Override
    @Transactional
    public CustomResponse<Boolean> validateOtp(String publicId, String target, String code) {
        Otp otpEntity = otpRepository.findByPublicId(publicId);
        if (otpEntity != null) {
            if (otpEntity.getCode().equals(code)
                    && otpEntity.getTarget().equals(target)
                    && !otpEntity.isExpired()
                    && otpEntity.getActive() == true) {
                otpEntity.setUsedAt(Instant.now());
                otpEntity.setActive(false);
                otpRepository.save(otpEntity);
                return CustomResponse.of(
                        HttpStatus.OK, true, AuthConstants.OTP_VERIFIED, SecurityConstants.VERIFY_OTP);
            } else {
                otpEntity.setFailedAttempts(otpEntity.getFailedAttempts() + 1);
                if (otpEntity.getFailedAttempts() >= SecurityConstants.OTP_MAX_ATTEMPTS) {
                    otpEntity.setActive(false);
                }
                if (otpEntity.isExpired()) {
                    otpEntity.setActive(false);
                }
                otpRepository.save(otpEntity);

                if (otpEntity.getFailedAttempts() >= SecurityConstants.OTP_MAX_ATTEMPTS) {
                    return CustomResponse.of(
                            HttpStatus.BAD_REQUEST,
                            false,
                            AuthConstants.OTP_MAX_ATTEMPTS,
                            SecurityConstants.VERIFY_OTP);
                } else if (otpEntity.isExpired()) {
                    return CustomResponse.of(
                            HttpStatus.BAD_REQUEST,
                            false,
                            AuthConstants.OTP_EXPIRED,
                            SecurityConstants.VERIFY_OTP);
                } else {
                    return CustomResponse.of(
                            HttpStatus.BAD_REQUEST,
                            false,
                            AuthConstants.OTP_NOT_VERIFIED,
                            SecurityConstants.VERIFY_OTP);
                }
            }
            // otpRepository.delete(otpEntity);
        } else {
            return CustomResponse.of(
                    HttpStatus.BAD_REQUEST,
                    false,
                    AuthConstants.OTP_NOT_FOUND,
                    SecurityConstants.VERIFY_OTP);
        }
    }
}
