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

import org.springframework.core.env.Environment;
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

    private static final String TEST_OTP_CODE = "190303";

    private final transient Environment environment;
    private final transient OtpRepository otpRepository;

    /**
     * Generates the otp code for the user with the given email or sms.
     *
     * @param target the target to email or sms
     * @return the generated otp code
     */
    @Override
    @Transactional
    public Otp generateOtp(String target) {
        Otp otpEntity = new Otp();

        otpEntity.setTarget(target);
        boolean isDevOrTest = isDevOrTestEnvironment();
        if (isDevOrTest) {
            otpEntity.setCode(TEST_OTP_CODE);
            LOG.info("Using test OTP code for development/test environment: {}", TEST_OTP_CODE);
        } else {
            otpEntity.setCode(Otp.generateOtp());
        }

        otpEntity.setActive(true);
        otpRepository.save(otpEntity);

        return otpEntity;
    }

    @Override
    @Transactional
    public Boolean validateOtp(Long id, String target, String code) {
        Otp otpEntity = otpRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(AuthConstants.OTP_NOT_FOUND));

        // Check if OTP is valid and matches
        boolean codeMatch = otpEntity.getCode().equals(code);
        boolean targetMatch = otpEntity.getTarget().equals(target);
        boolean isValid = otpEntity.isValid();

        if (codeMatch && targetMatch && isValid) {
            LOG.info("OTP validation successful! Marking as used.");
            otpEntity.setUsedAt(Instant.now());
            otpEntity.setActive(false);
            otpRepository.save(otpEntity);
            return true;
        }

        // Handle validation failure
        LOG.warn("OTP validation failed!");
        otpEntity.setFailedAttempts(otpEntity.getFailedAttempts() + 1);

        // Determine failure reason before saving
        String errorMessage;
        if (otpEntity.isExpired()) {
            otpEntity.setActive(false);
            errorMessage = AuthConstants.OTP_EXPIRED;
        } else if (otpEntity.getFailedAttempts() >= SecurityConstants.OTP_MAX_ATTEMPTS) {
            otpEntity.setActive(false);
            errorMessage = AuthConstants.OTP_MAX_ATTEMPTS;
        } else {
            errorMessage = AuthConstants.OTP_NOT_VERIFIED;
        }

        otpRepository.save(otpEntity);
        throw new IllegalArgumentException(errorMessage);
    }

    // Generates OTP and sends it via SMS
    @Override
    @Transactional
    public Map<String, Object> generateAndSendOtpSms(String phoneNumber) {
        LOG.info("Generating and sending OTP via SMS for phone: {}", phoneNumber);

        // Generate OTP entity
        Otp otp = generateOtp(phoneNumber);

        // Skip SMS sending in dev/test environments
        if (!isDevOrTestEnvironment()) {
            try {
                // NOTE: SMS Service integration required
                // Integrate with SMS provider (e.g., Twilio, AWS SNS) to send OTP
                // Example: smsService.sendOtp(phoneNumber, otp.getCode());
                LOG.warn("SMS Service not implemented yet. OTP code: {}", otp.getCode());

            } catch (Exception e) {
                LOG.error("Failed to send OTP SMS: {}", e.getMessage(), e);
                otp.setActive(false);
                otpRepository.save(otp);
                throw new RuntimeException("Failed to send OTP SMS: " + e.getMessage(), e);
            }
        } else {
            LOG.info("Skipping SMS sending in development/test environment");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", otp.getId());
        params.put("target", otp.getTarget());

        return params;
    }

    // Generates OTP and sends it via Email.
    @Override
    @Transactional
    public Map<String, Object> generateAndSendOtpEmail(String email) {
        LOG.info("Generating and sending OTP via Email for: {}", email);

        // Generate OTP entity
        Otp otp = generateOtp(email.trim());

        if(!isDevOrTestEnvironment()) {
            try {
                // NOTE: Email Service integration required
                // Integrate with email service (e.g., JavaMailSender, SendGrid) to send OTP email
                // Example: emailService.sendOtpEmail(email, otp.getCode());
                LOG.warn("Email Service not implemented yet. OTP code: {}", otp.getCode());

            } catch (Exception e) {
                LOG.error("Failed to send OTP Email: {}", e.getMessage(), e);
                otp.setActive(false);
                otpRepository.save(otp);
                throw new RuntimeException("Failed to send OTP Email: " + e.getMessage(), e);
            }
        } else {
            LOG.info("Skipping Email sending in development/test environment");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", otp.getId());
        params.put("target", otp.getTarget());

        return params;
    }

    /**
     * Checks if the current environment is development or test.
     *
     * @return true if running in development or test profile
     */
    private boolean isDevOrTestEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("development".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
