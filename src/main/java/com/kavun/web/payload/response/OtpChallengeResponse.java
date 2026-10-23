package com.kavun.web.payload.response;

/**
 * Returned by the login endpoint when a sign-in code was sent instead of tokens.
 *
 * @param id the challenge id to send to {@code /verify-otp} with the code
 * @param target where the code was sent, masked (e.g. {@code a***@example.com}), for display only
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public record OtpChallengeResponse(String id, String target) {}
