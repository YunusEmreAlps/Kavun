package com.kavun.backend.persistent.domain.user;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.kavun.TestUtils;
import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void equalsContract() {
    TestUtils.entityIdentityVerifier(Role.class).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(Role.class).withClassName(NameStyle.SIMPLE_NAME).verify();
  }
}
