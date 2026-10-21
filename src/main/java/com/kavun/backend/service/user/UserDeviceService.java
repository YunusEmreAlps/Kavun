package com.kavun.backend.service.user;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kavun.backend.persistent.domain.user.UserDevice;
import com.kavun.backend.persistent.repository.UserDeviceRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.DeviceDetectionService;
import com.kavun.constant.LoggingConstants;
import com.kavun.shared.dto.UserDeviceDto;
import com.kavun.shared.dto.mapper.UserDeviceMapper;
import com.kavun.shared.request.UserDeviceRequest;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Records the devices users sign in from.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 2.0
 */
@Slf4j
@Service
@Transactional
public class UserDeviceService extends
        AbstractService<UserDeviceRequest, UserDevice, UserDeviceDto, UserDeviceRepository, UserDeviceMapper> {

    private final DeviceDetectionService deviceDetectionService;

    public UserDeviceService(DeviceDetectionService deviceDetectionService, UserDeviceMapper mapper,
            UserDeviceRepository repository) {
        super(mapper, repository);
        this.deviceDetectionService = deviceDetectionService;
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of("userId", "deviceId", "deviceType", "operatingSystem", "browser", "lastSeenAt",
                "createdAt");
    }

    /**
     * Records a sign-in: refreshes the user's record of the device, or creates it on the first
     * sign-in from that device. The same browser used by two users gives two records.
     *
     * @param userId the user who signed in
     * @param deviceId the identifier the client sent for the device
     * @param request the sign-in request, for the browser and operating system
     */
    public void recordSignIn(Long userId, String deviceId, HttpServletRequest request) {
        UserDevice device = repository.findByUserIdAndDeviceId(userId, deviceId).orElseGet(() -> {
            var newDevice = new UserDevice();
            newDevice.setUserId(userId);
            newDevice.setDeviceId(deviceId);
            LOG.info("Registered new device ID {} for user: {}", deviceId, userId);
            return newDevice;
        });

        // Browser and operating system versions change with updates.
        DeviceDetectionService.DeviceInfo deviceInfo = deviceDetectionService.detect(request);
        device.setDeviceType(deviceInfo.getDeviceType());
        device.setOperatingSystem(deviceInfo.getOperatingSystem());
        device.setBrowser(deviceInfo.getBrowser());
        device.setUserAgent(request.getHeader(LoggingConstants.USER_AGENT_HEADER));
        device.setLastSeenAt(LocalDateTime.now());
        repository.save(device);
    }

    public Map<String, Long> getDeviceUsageAnalytics() {
        return repository.countDeviceTypes().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // device_type
                        row -> ((Number) row[1]).longValue() // count
                ));
    }
}
