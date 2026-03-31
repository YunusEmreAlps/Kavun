package com.kavun.backend.service.user;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.kavun.backend.persistent.domain.user.UserDevice;
import com.kavun.backend.persistent.repository.UserDeviceRepository;
import com.kavun.backend.persistent.specification.UserDeviceSpecification;
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
 * Service for managing user sessions.
 * Handles session creation, updates, and termination.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 2.0
 */
@Slf4j
@Service
@Transactional
public class UserDeviceService extends
        AbstractService<UserDeviceRequest, UserDevice, UserDeviceDto, UserDeviceRepository, UserDeviceMapper, UserDeviceSpecification> {

    private final DeviceDetectionService deviceDetectionService;

    public UserDeviceService(DeviceDetectionService deviceDetectionService, UserDeviceMapper mapper,
            UserDeviceRepository repository, UserDeviceSpecification specification) {
        super(mapper, repository, specification);
        this.deviceDetectionService = deviceDetectionService;
    }

    public void createDevice(Long userId, String deviceId, HttpServletRequest request) {
        String userAgent = request.getHeader(LoggingConstants.USER_AGENT_HEADER);
        DeviceDetectionService.DeviceInfo deviceInfo = deviceDetectionService.parseUserAgent(userAgent);

        // Check if device already exists
        Optional<UserDevice> existingDevice = repository.findByDeviceId(deviceId);

        if (existingDevice.isPresent()) {
            UserDevice device = existingDevice.get();

            // Update existing device information (browser, OS might change with updates)
            device.setDeviceType(deviceInfo.getDeviceType());
            device.setOperatingSystem(deviceInfo.getOperatingSystem());
            device.setBrowser(deviceInfo.getBrowser());
            device.setUserAgent(userAgent);

            repository.save(device);
            LOG.info("Updated device ID {} for user: {}", deviceId, userId);
        } else {
            // Create new device record
            UserDevice userDevice = new UserDevice();
            userDevice.setUserId(userId);
            userDevice.setDeviceId(deviceId);
            userDevice.setDeviceType(deviceInfo.getDeviceType());
            userDevice.setOperatingSystem(deviceInfo.getOperatingSystem());
            userDevice.setBrowser(deviceInfo.getBrowser());
            userDevice.setUserAgent(userAgent);

            repository.save(userDevice);
            LOG.info("Registered new device ID {} for user: {}", deviceId, userId);
        }
    }

    public Specification<UserDevice> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }

    public Map<String, Long> getDeviceUsageAnalytics() {
        return repository.countDeviceTypes().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // device_type
                        row -> ((Number) row[1]).longValue() // count
                ));
    }
}
