package com.kavun.shared.util.core;

import com.kavun.backend.service.impl.UserDetailsBuilder;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantContextTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void workWithoutUserOrBlockIsRoot() {
    Assertions.assertEquals(TenantContext.ROOT, TenantContext.current());
    Assertions.assertFalse(TenantContext.isPlatform());
  }

  @Test
  void signedInUserDeterminesTheTenant() {
    signIn("acme");

    Assertions.assertEquals("acme", TenantContext.current());
  }

  @Test
  void explicitBlockWinsAndIsRestored() {
    signIn("acme");

    TenantContext.runAs(
        "globex",
        () -> {
          Assertions.assertEquals("globex", TenantContext.current());
          TenantContext.runAs(
              TenantContext.PLATFORM, () -> Assertions.assertTrue(TenantContext.isPlatform()));
          Assertions.assertEquals("globex", TenantContext.current());
        });

    Assertions.assertEquals("acme", TenantContext.current());
  }

  @Test
  void nullBlockFallsBackToTheSignedInUser() {
    signIn("acme");

    String inside =
        TenantContext.callAs(
            TenantContext.PLATFORM, () -> TenantContext.callAs(null, TenantContext::current));

    Assertions.assertEquals("acme", inside);
  }

  @Test
  void blockIsRestoredWhenWorkFails() {
    Assertions.assertThrows(
        IllegalStateException.class,
        () ->
            TenantContext.runAs(
                "acme",
                () -> {
                  throw new IllegalStateException("boom");
                }));

    Assertions.assertEquals(TenantContext.ROOT, TenantContext.current());
  }

  private static void signIn(String tenantCode) {
    var user = UserDetailsBuilder.builder().id(1L).username("user").tenantCode(tenantCode).build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
  }
}
