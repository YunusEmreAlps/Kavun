package com.kavun.backend.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * Service for detecting device information from User-Agent strings.
 * Uses UAParser library to extract device type, OS, and browser information.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 2.0
 */
@Slf4j
@Service
public class DeviceDetectionService {

    private final Parser uaParser;

    public DeviceDetectionService() {
        this.uaParser = new Parser();
    }

    /**
     * Parses User-Agent string and returns device information.
     *
     * @param userAgent User-Agent header value
     * @return DeviceInfo object containing parsed information
     */
    public DeviceInfo parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceInfo.unknown();
        }

        try {
            Client client = uaParser.parse(userAgent);

            String deviceType = determineDeviceType(client);
            String os = formatOperatingSystem(client);
            String browser = formatBrowser(client);

            return new DeviceInfo(deviceType, os, browser);
        } catch (Exception e) {
            LOG.warn("Failed to parse User-Agent: {}", e.getMessage());
            return DeviceInfo.unknown();
        }
    }

    /**
     * Determines device type based on parsed client information.
     */
    private String determineDeviceType(Client client) {
        String deviceFamily = client.device.family;

        if (deviceFamily == null || "Other".equalsIgnoreCase(deviceFamily)) {
            // Check for mobile HTTP clients (OkHttp, Retrofit, etc.)
            if (isMobileHttpClient(client)) {
                return "MOBILE";
            }

            // Check OS for mobile indicators
            String osFamily = client.os.family;
            if (osFamily != null) {
                String lowerOs = osFamily.toLowerCase();
                if (lowerOs.contains("android") || lowerOs.contains("ios")) {
                    return "MOBILE";
                }
            }
            return "WEB";
        }

        String lowerDevice = deviceFamily.toLowerCase();

        if (lowerDevice.contains("spider") || lowerDevice.contains("bot")) {
            return "BOT";
        }
        if (lowerDevice.contains("tablet") || lowerDevice.contains("ipad")) {
            return "TABLET";
        }
        if (lowerDevice.contains("mobile") || lowerDevice.contains("phone") ||
            lowerDevice.contains("iphone") || lowerDevice.contains("android")) {
            return "MOBILE";
        }
        if (lowerDevice.contains("tv") || lowerDevice.contains("console")) {
            return "TV";
        }

        return "WEB";
    }

    /**
     * Checks if the user agent represents a mobile HTTP client library.
     */
    private boolean isMobileHttpClient(Client client) {
        if (client.userAgent == null || client.userAgent.family == null) {
            return false;
        }

        String userAgentFamily = client.userAgent.family.toLowerCase();

        // Android HTTP clients
        if (userAgentFamily.contains("okhttp") ||
            userAgentFamily.contains("retrofit") ||
            userAgentFamily.contains("volley") ||
            userAgentFamily.contains("androidasynchttp")) {
            return true;
        }

        // iOS HTTP clients
        if (userAgentFamily.contains("alamofire") ||
            userAgentFamily.contains("afnetworking") ||
            userAgentFamily.contains("nsurlsession")) {
            return true;
        }

        return false;
    }

    /**
     * Formats operating system information.
     */
    private String formatOperatingSystem(Client client) {
        if (client.os == null || client.os.family == null) {
            return "Unknown";
        }

        StringBuilder sb = new StringBuilder(client.os.family);

        if (client.os.major != null) {
            sb.append(" ").append(client.os.major);
            if (client.os.minor != null) {
                sb.append(".").append(client.os.minor);
            }
        }

        return sb.toString();
    }

    /**
     * Formats browser information.
     */
    private String formatBrowser(Client client) {
        if (client.userAgent == null || client.userAgent.family == null) {
            return "Unknown";
        }

        StringBuilder sb = new StringBuilder(client.userAgent.family);

        if (client.userAgent.major != null) {
            sb.append(" ").append(client.userAgent.major);
            if (client.userAgent.minor != null) {
                sb.append(".").append(client.userAgent.minor);
            }
        }

        return sb.toString();
    }

    /**
     * Device information DTO.
     */
    @Getter
    public static class DeviceInfo {
        private final String deviceType;
        private final String operatingSystem;
        private final String browser;

        public DeviceInfo(String deviceType, String operatingSystem, String browser) {
            this.deviceType = deviceType;
            this.operatingSystem = operatingSystem;
            this.browser = browser;
        }

        public static DeviceInfo unknown() {
            return new DeviceInfo("UNKNOWN", "Unknown", "Unknown");
        }

        @Override
        public String toString() {
            return String.format("DeviceInfo{type=%s, os=%s, browser=%s}",
                    deviceType, operatingSystem, browser);
        }
    }
}
