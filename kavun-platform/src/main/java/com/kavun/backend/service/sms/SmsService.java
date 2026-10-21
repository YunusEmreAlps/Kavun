package com.kavun.backend.service.sms;

/**
 * Service interface for sending SMS messages.
 * Provides operations for sending OTP and other SMS notifications.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public interface SmsService {

  /**
   * Sends an SMS message to the specified phone number.
   *
   * @param phoneNumber the recipient phone number (E.164 format recommended, e.g., +905551234567)
   * @param message the message content to send
   * @throws com.kavun.exception.user.SmsServiceException if SMS sending fails
   */
  void sendSms(String phoneNumber, String message);

  /**
   * Sends OTP (One-Time Password) via SMS to the specified phone number.
   *
   * @param phoneNumber the recipient phone number (E.164 format recommended)
   * @param otpCode the OTP code to send
   * @throws com.kavun.exception.user.SmsServiceException if SMS sending fails
   */
  void sendOtpSms(String phoneNumber, String otpCode);

  /**
   * Validates phone number format.
   * Basic validation for Turkish phone numbers.
   *
   * @param phoneNumber the phone number to validate
   * @return true if valid, false otherwise
   */
  boolean isValidPhoneNumber(String phoneNumber);
}
