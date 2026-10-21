package com.kavun.backend.service.security.impl;

import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.security.BruteForceProtectionService;
import com.kavun.constant.CacheConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.util.core.ValidationUtils;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is the implementation of the brute force protection service.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class BruteForceProtectionServiceImpl implements BruteForceProtectionService {

  @Value("${security.failedLoginAttempts}")
  private int maxFailedLogins;

  private final UserRepository userRepository;

  public BruteForceProtectionServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(value = CacheConstants.USERS, key = "#username"),
        @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
      })
  @Transactional
  public void registerLoginFailure(final String username) {
    ValidationUtils.validateInputs(username, UserConstants.BLANK_USERNAME);

    var user = userRepository.findByUsername(username);

    if (Objects.nonNull(user) && user.isAccountNonLocked()) {
      var failedAttempts = user.getFailedLoginAttempts();
      if (maxFailedLogins < failedAttempts + 1) {
        LOG.info("User {} is locked due to {} failed login attempts", username, failedAttempts);
        // disable user account
        user.setAccountNonLocked(false);
      } else {
        // increment failed login attempts
        user.setFailedLoginAttempts(failedAttempts + 1);
        LOG.debug("User {} has {} failed login attempts", username, failedAttempts + 1);
      }
    } else {
      LOG.warn("User {} is not found or is already locked", username);
    }
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(value = CacheConstants.USERS, key = "#username"),
        @CacheEvict(value = CacheConstants.USER_DETAILS, allEntries = true)
      })
  @Transactional
  public void resetBruteForceCounter(final String username) {
    ValidationUtils.validateInputs(username, UserConstants.BLANK_USERNAME);

    var user = userRepository.findByUsername(username);

    if (Objects.nonNull(user)) {
      user.setFailedLoginAttempts(0);
      user.setLastSuccessfulLogin(LocalDateTime.now());
      // if the user is not locked, then there is no need to update the accountNonLocked field
      if (!user.isAccountNonLocked()) {
        user.setAccountNonLocked(true);
      }
    } else {
      LOG.warn("User {} is not found", username);
    }
  }

  @Override
  public boolean isBruteForceAttack(final String username) {
    ValidationUtils.validateInputs(username, UserConstants.BLANK_USERNAME);

    if (userRepository.existsByUsernameAndFailedLoginAttemptsGreaterThan(
        username, maxFailedLogins)) {

      LOG.debug(
          "Possible bruteforce attack on username {} and maxFailedLogins {}",
          username,
          maxFailedLogins);
      return true;
    }
    return false;
  }
}
