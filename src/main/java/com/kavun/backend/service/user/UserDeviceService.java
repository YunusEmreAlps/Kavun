package com.kavun.backend.service.user;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kavun.backend.persistent.domain.user.UserDevice;
import com.kavun.backend.persistent.repository.UserDeviceRepository;
import com.kavun.backend.service.DeviceDetectionService;
import com.kavun.constant.LoggingConstants;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final DeviceDetectionService deviceDetectionService;

    @Transactional
    public void createDevice(Long userId, String deviceId, HttpServletRequest request) {
        String userAgent = request.getHeader(LoggingConstants.USER_AGENT_HEADER);
        DeviceDetectionService.DeviceInfo deviceInfo = deviceDetectionService.parseUserAgent(userAgent);

        // Check if device already exists
        Optional<UserDevice> existingDevice = userDeviceRepository.findByDeviceId(deviceId);

        if (existingDevice.isPresent()) {
            UserDevice device = existingDevice.get();

            // Update existing device information (browser, OS might change with updates)
            device.setDeviceType(deviceInfo.getDeviceType());
            device.setOperatingSystem(deviceInfo.getOperatingSystem());
            device.setBrowser(deviceInfo.getBrowser());
            device.setUserAgent(userAgent);

            userDeviceRepository.save(device);
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

            userDeviceRepository.save(userDevice);
            LOG.info("Registered new device ID {} for user: {}", deviceId, userId);
        }
    }
}
