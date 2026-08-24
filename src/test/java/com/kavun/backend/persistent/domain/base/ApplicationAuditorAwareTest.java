package com.kavun.backend.persistent.domain.base;

import com.kavun.TestUtils;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.shared.util.UserUtils;

import java.util.List;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApplicationAuditorAwareTest {

  private static final Long ADMIN_USER_ID = 7L;

  @Mock private transient UserRepository userRepository;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentAuditorWithNoAuthentication() {
    var adminUser = new User();
    adminUser.setId(ADMIN_USER_ID);
    Mockito.when(userRepository.findByUsername("admin")).thenReturn(adminUser);

    var applicationAuditorAware = new ApplicationAuditorAware(userRepository);
    Assertions.assertEquals(Optional.of(ADMIN_USER_ID), applicationAuditorAware.getCurrentAuditor());
  }

  @Test
  void getCurrentAuditorWithAnonymousUser() {
    TestUtils.setAuthentication(TestUtils.ANONYMOUS_USER, TestUtils.ANONYMOUS_ROLE);
    SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);

    var adminUser = new User();
    adminUser.setId(ADMIN_USER_ID);
    Mockito.when(userRepository.findByUsername("admin")).thenReturn(adminUser);

    var applicationAuditorAware = new ApplicationAuditorAware(userRepository);
    Assertions.assertEquals(Optional.of(ADMIN_USER_ID), applicationAuditorAware.getCurrentAuditor());
  }

  @Test
  void getCurrentAuditorFallsBackToDefaultWhenAdminUserNotFound() {
    Mockito.when(userRepository.findByUsername("admin")).thenReturn(null);

    var applicationAuditorAware = new ApplicationAuditorAware(userRepository);
    Assertions.assertEquals(Optional.of(1L), applicationAuditorAware.getCurrentAuditor());
  }

  @Test
  void getCurrentAuditorWithAuthenticatedUser(TestInfo testInfo) {
    var user = UserUtils.createUser(testInfo.getDisplayName());
    user.setId(42L);
    var principal = UserDetailsBuilder.buildUserDetails(user);
    var authorities = List.of(new SimpleGrantedAuthority(TestUtils.ROLE_USER));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));

    var applicationAuditorAware = new ApplicationAuditorAware(userRepository);
    Assertions.assertEquals(Optional.of(42L), applicationAuditorAware.getCurrentAuditor());
  }

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(ApplicationAuditorAware.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }
}
