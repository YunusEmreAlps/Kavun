package com.kavun.shared.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaStore {

  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final long BLOCK_DURATION_SECONDS = 3 * 60;
  private static final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();
  private static final Map<String, LoginAttempts> attemptsStore = new ConcurrentHashMap<>();

  public static String saveCaptcha(String code) {
    String captchaId = UUID.randomUUID().toString();
    store.put(captchaId, new CaptchaEntry(code, Instant.now()));
    return captchaId;
  }

  public static boolean validateCaptcha(String captchaId, String userInput) {
    CaptchaEntry entry = store.get(captchaId);
    if (entry == null) return false;

    /*if (Instant.now().getEpochSecond() - entry.createdAt.getEpochSecond() > EXPIRATION_TIME) {
      store.remove(captchaId);
      return false;
    }

    if (entry.attemptCount >= MAX_CAPTCHA_ATTEMPTS) {
      store.remove(captchaId);
      return false;
    }*/

    if (entry.code.equals(userInput)) {
      store.remove(captchaId);
      return true;
    } else {
      entry.incrementAttempt();
      return false;
    }
  }

  public static void saveFailure(String username) {
    attemptsStore.compute(username, (k, v) -> {
      if (v == null) v = new LoginAttempts();
      v.count++;
      v.lastAttempt = Instant.now();
      return v;
    });
  }

  public static boolean isBlocked(String username) {
    LoginAttempts attempt = attemptsStore.get(username);
    if (attempt == null) return false;

    if (attempt.count >= MAX_LOGIN_ATTEMPTS) {
      long secondsSinceLastAttempt = Duration.between(attempt.lastAttempt, Instant.now()).getSeconds();
      if (secondsSinceLastAttempt < BLOCK_DURATION_SECONDS) {
        return true;
      } else {
        attempt.count = 0;
        attempt.lastAttempt = Instant.now();
        return false;
      }
    }
    return false;
  }

  public static void saveSuccess(String username) {
    attemptsStore.remove(username);
  }

  private static class CaptchaEntry {
    String code;
    Instant createdAt;
    int attemptCount;

    CaptchaEntry(String code, Instant createdAt) {
      this.code = code;
      this.createdAt = createdAt;
      this.attemptCount = 0;
    }

    void incrementAttempt() {
      this.attemptCount++;
    }
  }

  private static class LoginAttempts {
    int count = 0;
    Instant lastAttempt = Instant.now();
  }
}
