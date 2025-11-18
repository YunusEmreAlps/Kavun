package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Otp;
import com.kavun.backend.persistent.repository.OtpRepository;
import com.kavun.backend.service.user.OtpService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.SecurityConstants;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public Map<String, Object> generateOtp(String target) {
        Otp otpEntity = new Otp();

        otpEntity.setTarget(target);
        otpEntity.setCode(Otp.generateOtp());
        otpEntity.setActive(true);
        otpRepository.save(otpEntity);

        Map<String, Object> params = new HashMap<>();
        params.put("publicId", otpEntity.getPublicId());
        params.put("target", otpEntity.getTarget());

        return params;
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
    public Boolean validateOtp(String publicId, String target, String code) {
        Otp otpEntity = otpRepository.findByPublicId(publicId);
        if (otpEntity != null) {
            if (otpEntity.getCode().equals(code)
                    && otpEntity.getTarget().equals(target)
                    && !otpEntity.isExpired()
                    && otpEntity.getActive() == true) {
                otpEntity.setUsedAt(Instant.now());
                otpEntity.setActive(false);
                otpRepository.save(otpEntity);
                return true;
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
                    throw new IllegalArgumentException(AuthConstants.OTP_MAX_ATTEMPTS);
                } else if (otpEntity.isExpired()) {
                    throw new IllegalArgumentException(AuthConstants.OTP_EXPIRED);
                } else {
                    throw new IllegalArgumentException(AuthConstants.OTP_NOT_VERIFIED);
                }
            }
            // otpRepository.delete(otpEntity);
        } else {
            throw new IllegalArgumentException(AuthConstants.OTP_NOT_FOUND);
        }
    }
}
