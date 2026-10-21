package com.kavun.task;

import com.kavun.IntegrationTestUtils;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.auditing.CurrentDateTimeProvider;

class UserPruningSchedulerIntegrationTest extends IntegrationTestUtils {

  @BeforeEach
  void setUp() {
    auditingHandler.setDateTimeProvider(dateTimeProvider);
  }

  // The auditing handler is shared by the whole cached test context; restore the real clock so
  // entities created by later test classes still get their audit timestamps.
  @AfterEach
  void tearDown() {
    auditingHandler.setDateTimeProvider(CurrentDateTimeProvider.INSTANCE);
  }

  @Test
  void findAllNotEnabledAfterAllowedDays() {
    // Create a fixed clock to 60 days back in time.
    var instantExpected = LocalDateTime.now().minusDays(60).toInstant(ZoneOffset.UTC).toString();
    var fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.systemDefault());

    // When dateTimeProvider is called, return fixedClock to simulate creating user in the past.
    Mockito.when(dateTimeProvider.getNow()).thenReturn(Optional.of(LocalDateTime.now(fixedClock)));

    UserDto userDto = createAndAssertUser(UserUtils.createUserDto(false));

    var users = userService.findAllNotEnabledAfterAllowedDays();
    Assertions.assertFalse(users.isEmpty());
    Assertions.assertTrue(users.stream().anyMatch(user -> user.getId().equals(userDto.getId())));

    // Pruning should remove the user
    userPruningScheduler.pruneUsers();

    // Assert that the user is no longer in the database
    Assertions.assertFalse(userService.existsByUsername(userDto.getUsername()));
  }
}
