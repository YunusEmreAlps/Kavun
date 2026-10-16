package com.kavun.backend.service.impl;

import com.kavun.backend.persistent.domain.tenant.Tenant;
import com.kavun.backend.persistent.repository.TenantRepository;
import java.util.Optional;
import org.mockito.ArgumentMatchers;
import org.springframework.security.authentication.DisabledException;
import com.kavun.TestUtils;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.shared.util.UserUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UserDetailsServiceImplTest extends TestUtils {

  @Mock private transient UserRepository userRepository;

  @Mock private transient TenantRepository tenantRepository;

  @InjectMocks private transient UserDetailsServiceImpl userDetailsService;

  private transient String email;

  @BeforeEach
  void setUp(TestInfo testInfo) throws Exception {
    try (var mocks = MockitoAnnotations.openMocks(this)) {
      Assertions.assertNotNull(mocks);

      email = FAKER.internet().emailAddress();
      var user =
          UserUtils.createUser(testInfo.getDisplayName(), FAKER.internet().password(), email);

      Mockito.when(userRepository.findByUsernameAndDeletedFalse(testInfo.getDisplayName()))
          .thenReturn(user);
      Mockito.when(userRepository.findByEmailAndDeletedFalse(user.getEmail())).thenReturn(user);
      Mockito.when(tenantRepository.findByCode(ArgumentMatchers.any()))
          .thenReturn(Optional.of(new Tenant()));
    }
  }

  @Test
  void testShouldRejectUserOfDisabledTenant(TestInfo testInfo) {
    var disabled = new Tenant();
    disabled.setEnabled(false);
    Mockito.when(tenantRepository.findByCode(ArgumentMatchers.any()))
        .thenReturn(Optional.of(disabled));

    Assertions.assertThrows(
        DisabledException.class,
        () -> userDetailsService.loadUserByUsername(testInfo.getDisplayName()));
  }

  @Test
  void testShouldThrowGivenNullInput() {
    Assertions.assertThrows(
        UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(null));
  }

  @Test
  void testShouldReturnUserGivenAnExistingUsername(TestInfo testInfo) {
    var userDetails = userDetailsService.loadUserByUsername(testInfo.getDisplayName());
    Assertions.assertNotNull(userDetails);
    Assertions.assertEquals(testInfo.getDisplayName(), userDetails.getUsername());
  }

  @Test
  void testShouldThrowExceptionForNonExistingUsername() {
    Assertions.assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(FAKER.internet().username()));
  }

  @Test
  void testShouldReturnUserGivenAnExistingEmail(TestInfo testInfo) {
    var userDetails = userDetailsService.loadUserByUsername(email);
    Assertions.assertNotNull(userDetails);
    Assertions.assertEquals(testInfo.getDisplayName(), userDetails.getUsername());
  }
}
