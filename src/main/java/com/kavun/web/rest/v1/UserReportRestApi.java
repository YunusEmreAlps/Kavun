package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.siem.ApplicationLogService;
import com.kavun.backend.service.user.UserDeviceService;
import com.kavun.backend.service.user.UserSessionService;
import com.kavun.constant.base.BaseConstants;
import com.kavun.shared.dto.ApplicationLogDto;
import com.kavun.shared.dto.UserDeviceDto;
import com.kavun.shared.dto.UserSessionDto;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * This class handles all rest calls for user session and device reports.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(BaseConstants.API_V1_USERS_ROOT_URL + "/reports")
@Tag(name = "15. User Reports", description = "APIs for user session reporting and analytics")
public class UserReportRestApi {

    private final UserDeviceService userDeviceService;
    private final UserSessionService userSessionService;
    private final ApplicationLogService applicationLogService;

    // Retrieves user sessions with dynamic filtering and pagination.
    @Loggable
    @GetMapping(value = "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<UserSessionDto> getUserSessions(
            @RequestParam Map<String, Object> parameterMap,
            @PageableDefault(size = 20, sort = "loginAt") Pageable pageable) {

        return userSessionService.findAll(
                userSessionService.search(parameterMap),
                pageable);
    }

    // Retrieves user devices with dynamic filtering and pagination.
    @Loggable
    @GetMapping(value = "/devices", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<UserDeviceDto> getUserDevices(
            @RequestParam Map<String, Object> parameterMap,
            @PageableDefault(size = 20) Pageable pageable) {

        return userDeviceService.findAll(
                userDeviceService.search(parameterMap),
                pageable);
    }

    // Retrieves device usage analytics for all user devices.
    @Loggable
    @GetMapping(value = "/device-analytics", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> getDeviceUsageAnalytics() {
        return userDeviceService.getDeviceUsageAnalytics();
    }

    // Get user activity logs with dynamic filtering and pagination.
    @Loggable
    @GetMapping(value = "/activity-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ApplicationLogDto> getUserActivityLogs(
            @RequestParam Map<String, Object> parameterMap,
            @PageableDefault(size = 20) Pageable pageable) {

        return applicationLogService.findAll(
                applicationLogService.search(parameterMap),
                pageable);
    }
}
