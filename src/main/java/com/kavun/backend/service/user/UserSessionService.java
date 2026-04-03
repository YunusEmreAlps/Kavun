package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.UserSession;
import com.kavun.backend.persistent.repository.UserSessionRepository;
import com.kavun.backend.persistent.specification.UserSessionSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.backend.service.DeviceDetectionService;
import com.kavun.backend.service.DeviceDetectionService.DeviceInfo;
import com.kavun.constant.LoggingConstants;
import com.kavun.shared.dto.UserSessionDto;
import com.kavun.shared.dto.mapper.UserSessionMapper;
import com.kavun.shared.request.UserSessionRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
public class UserSessionService extends
        AbstractService<UserSessionRequest, UserSession, UserSessionDto, UserSessionRepository, UserSessionMapper, UserSessionSpecification> {

    @Value("${access-token-expiration-in-minutes:60}")
    private long sessionTimeoutMinutes;

    private final DeviceDetectionService deviceDetectionService;

    public UserSessionService(UserSessionRepository repository,
                              DeviceDetectionService deviceDetectionService,
                              UserSessionMapper mapper,
                              UserSessionSpecification specification) {
        super(mapper, repository, specification);
        this.deviceDetectionService = deviceDetectionService;
    }

    // Creates a new user session from HTTP request.
    @Transactional
    public UserSession createSession(Long userId, HttpServletRequest request) {
        return createSession(userId, request, null);
    }

    // Creates a new user session with optional refresh token.
    @Transactional
    public UserSession createSession(Long userId, HttpServletRequest request, String refreshTokenHash) {
        String userAgent = request.getHeader(LoggingConstants.USER_AGENT_HEADER);
        String deviceId = request.getHeader(LoggingConstants.DEVICE_ID_HEADER);
        String ipAddress = extractIpAddress(request);

        DeviceInfo deviceInfo = deviceDetectionService.parseUserAgent(userAgent);

        // Check if session already exists for this device
        Optional<UserSession> existingSession = repository
                .findByUserIdAndDeviceIdAndIsActiveTrue(userId, deviceId);

        if (existingSession.isPresent()) {
            // Reactivate existing session
            UserSession session = existingSession.get();
            session.setLoginAt(LocalDateTime.now());
            session.setLastActivityAt(LocalDateTime.now());
            session.setLogoutAt(null);
            session.setIsActive(true);
            session.setRefreshTokenHash(refreshTokenHash);
            session.setIpAddress(ipAddress);
            session.setLogoutType(null);

            LOG.debug("Reactivated existing session for user {} on device {}", userId, deviceId);
            return repository.save(session);
        }

        // Create new session
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setDeviceId(deviceId);
        session.setDeviceType(deviceInfo.getDeviceType());
        session.setOperatingSystem(deviceInfo.getOperatingSystem());
        session.setBrowser(deviceInfo.getBrowser());
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setRefreshTokenHash(refreshTokenHash);

        UserSession savedSession = repository.save(session);
        LOG.info("Created new session {} for user {} from device {} ({})",
                savedSession.getId(), userId, deviceId, deviceInfo.getDeviceType());

        return savedSession;
    }

    // Updates the last activity timestamp for a session.
    @Transactional
    public void updateActivity(Long sessionId) {
        repository.updateLastActivity(sessionId, LocalDateTime.now());
    }

    // Logs out a specific session (manual logout).
    @Transactional
    public void logout(Long sessionId) {
        repository.findById(sessionId).ifPresent(session -> {
            session.logout();
            repository.save(session);
            LOG.info("User {} manually logged out from session {}", session.getUserId(), sessionId);
        });
    }

    // Logs out a user from a specific device.
    @Transactional
    public void logoutFromDevice(Long userId, String deviceId) {
        repository.findByUserIdAndDeviceIdAndIsActiveTrue(userId, deviceId)
                .ifPresent(session -> {
                    session.logout();
                    repository.save(session);
                    LOG.info("User {} logged out from device {}", userId, deviceId);
                });
    }

    // Force logout from all devices (e.g., password change, security event).
    @Transactional
    public int logoutFromAllDevices(Long userId) {
        int count = repository.deactivateAllUserSessions(userId, LocalDateTime.now());
        LOG.info("Force logged out user {} from {} devices", userId, count);
        return count;
    }

    // Gets all active sessions for a user.
    @Transactional(readOnly = true)
    public List<UserSession> getActiveSessions(Long userId) {
        return repository.findByUserIdAndIsActiveTrueOrderByLoginAtDesc(userId);
    }

    // Gets all sessions (active and inactive) for a user.
    @Transactional(readOnly = true)
    public List<UserSession> getAllSessions(Long userId) {
        return repository.findByUserIdOrderByLoginAtDesc(userId);
    }

    // Monthly total session duration analytics for all users.
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlySessionDurationTrends(Integer year) {
        year = (year == null) ? LocalDateTime.now().getYear() : year;
        return repository.countMonthlySessionDurations(year);
    }

    // Finds session by refresh token hash.
    @Transactional(readOnly = true)
    public Optional<UserSession> findByRefreshToken(String refreshTokenHash) {
        return repository.findByRefreshTokenHashAndIsActiveTrue(refreshTokenHash);
    }

    // Checks if a session is expired.
    public boolean isExpired(UserSession session) {
        return session.isExpired(sessionTimeoutMinutes);
    }

    // Extracts IP address from HTTP request.
    private String extractIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Get first IP if multiple IPs are present
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    // Search sessions based on dynamic criteria.
    public Specification<UserSession> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }
}
