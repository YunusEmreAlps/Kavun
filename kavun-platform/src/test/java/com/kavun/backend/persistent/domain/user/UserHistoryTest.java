package com.kavun.backend.persistent.domain.user;

import com.kavun.TestUtils;
import com.kavun.shared.util.UserUtils;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import org.junit.jupiter.api.Test;

class UserHistoryTest {

  @Test
  void equalsContract() {

    User client = UserUtils.createUser();
    User admin = UserUtils.createUser();

    TestUtils.entityIdentityVerifier(UserHistory.class)
        .withPrefabValues(User.class, client, admin)
        .verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(UserHistory.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .withIgnoredFields("user")
        .verify();
  }
}
