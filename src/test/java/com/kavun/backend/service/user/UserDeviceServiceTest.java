package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.UserDevice;
import com.kavun.backend.persistent.repository.UserDeviceRepository;
import com.kavun.backend.service.DeviceDetectionService;
import com.kavun.constant.LoggingConstants;
import com.kavun.shared.dto.mapper.UserDeviceMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

  private static final String DEVICE_ID = "web-5465f34da078da5bbccf979b7e682f9c";
  private static final String CHROME_ON_WINDOWS =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
          + "Chrome/155.0.0.0 Safari/537.36";

  @Mock private transient UserDeviceRepository repository;

  private transient UserDeviceService userDeviceService;

  private transient MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    userDeviceService =
        new UserDeviceService(
            new DeviceDetectionService(), Mappers.getMapper(UserDeviceMapper.class), repository);
    request = new MockHttpServletRequest();
    request.addHeader(LoggingConstants.USER_AGENT_HEADER, CHROME_ON_WINDOWS);
  }

  @Test
  void firstSignInFromADeviceCreatesTheUsersRecordOfIt() {
    Mockito.when(repository.findByUserIdAndDeviceId(2L, DEVICE_ID)).thenReturn(Optional.empty());

    userDeviceService.recordSignIn(2L, DEVICE_ID, request);

    var saved = ArgumentCaptor.forClass(UserDevice.class);
    Mockito.verify(repository).save(saved.capture());
    Assertions.assertEquals(2L, saved.getValue().getUserId());
    Assertions.assertEquals(DEVICE_ID, saved.getValue().getDeviceId());
    Assertions.assertTrue(saved.getValue().getBrowser().startsWith("Chrome"));
    Assertions.assertNotNull(saved.getValue().getLastSeenAt());
  }

  @Test
  void signingInAgainRefreshesTheSameRecord() {
    var lastWeek = LocalDateTime.now().minusWeeks(1);
    var device = new UserDevice();
    device.setUserId(1L);
    device.setDeviceId(DEVICE_ID);
    device.setBrowser("Chrome 154.0");
    device.setLastSeenAt(lastWeek);
    Mockito.when(repository.findByUserIdAndDeviceId(1L, DEVICE_ID)).thenReturn(Optional.of(device));

    userDeviceService.recordSignIn(1L, DEVICE_ID, request);

    Mockito.verify(repository).save(device);
    Assertions.assertTrue(device.getLastSeenAt().isAfter(lastWeek));
    Assertions.assertTrue(device.getBrowser().startsWith("Chrome 155"));
  }
}
