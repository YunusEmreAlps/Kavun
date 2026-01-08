package com.kavun.backend.persistent.domain.base;

import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.shared.util.core.SecurityUtils;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;

/**
 * This class gets the application's current auditor which is the username of the authenticated
 * user.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public final class ApplicationAuditorAware implements AuditorAware<Long> {

  private final UserRepository userRepository;
  private Long adminUserId = null; // Cache admin user ID

  /**
   * Returns the current auditor of the application.
   *
   * @return the current auditor
   */
  @NonNull
  @Override
  public Optional<Long> getCurrentAuditor() {

    // Check if there is a user logged in.
    // If so, use the logged-in user as the current auditor.
    // spring injects an anonymousUser if there is no
    // authentication and authorization
    var authentication = SecurityUtils.getAuthentication();
    if (SecurityUtils.isAuthenticated(authentication)) {
      // Set ID
      return Optional.of(SecurityUtils.getAuthorizedUserDetails().getId());
    }

    // If there is no authentication,
    // then the admin user will be used as the current auditor.
    return Optional.of(getAdminUserId());
  }

  /**
   * Get admin user ID, cached for performance
   */
  private Long getAdminUserId() {
    if (adminUserId == null) {
      var adminUser = userRepository.findByUsername("admin");
      if (adminUser != null) {
        adminUserId = adminUser.getId();
      } else {
        // Fallback to system ID if admin not found
        adminUserId = 0L;
      }
    }
    return adminUserId;
  }
}
