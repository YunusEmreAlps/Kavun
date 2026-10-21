package com.kavun.backend.service;

import static com.kavun.constant.LoggingConstants.DEVICE_TYPE_HEADER;
import static com.kavun.constant.LoggingConstants.SEC_CH_UA_MOBILE_HEADER;
import static com.kavun.constant.LoggingConstants.USER_AGENT_HEADER;

import com.kavun.enums.DeviceType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    private static final Set<DeviceType> CLIENT_REPORTABLE_TYPES =
            Set.of(DeviceType.DESKTOP, DeviceType.TABLET, DeviceType.MOBILE, DeviceType.TV);

    private static final List<String> NATIVE_HTTP_CLIENT_MARKERS = List.of(
            // Android
            "okhttp", "retrofit", "volley", "androidasynchttp", "dalvik",
            // iOS
            "alamofire", "afnetworking", "nsurlsession", "cfnetwork",
            // Flutter
            "dart/", "dart:io");

    private final Parser uaParser;

    public DeviceDetectionService() {
        this.uaParser = new Parser();
    }

    /**
     * Detects device information from an HTTP request. Besides the User-Agent, this honours
     * the client-supplied {@code X-Device-Type} header and the {@code Sec-CH-UA-Mobile} client
     * hint, which are the only ways to recognise devices whose User-Agent hides the form factor
     * (iPadOS Safari reports itself as a Mac, Chrome on Android sends a reduced User-Agent).
     *
     * @param request the HTTP request
     * @return DeviceInfo object containing parsed information
     */
    public DeviceInfo detect(HttpServletRequest request) {
        return parse(
                request.getHeader(USER_AGENT_HEADER),
                request.getHeader(DEVICE_TYPE_HEADER),
                request.getHeader(SEC_CH_UA_MOBILE_HEADER));
    }

    /**
     * Parses User-Agent string and returns device information.
     *
     * @param userAgent User-Agent header value
     * @return DeviceInfo object containing parsed information
     */
    public DeviceInfo parseUserAgent(String userAgent) {
        return parse(userAgent, null, null);
    }

    private DeviceInfo parse(String userAgent, String deviceTypeHint, String secChUaMobile) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceInfo.unknown();
        }

        try {
            Client client = uaParser.parse(userAgent);

            DeviceType deviceType = parseDeviceTypeHint(deviceTypeHint);
            if (deviceType == null) {
                deviceType = determineDeviceType(client, userAgent, secChUaMobile);
            }
            String os = formatOperatingSystem(client);
            String browser = formatBrowser(client);

            return new DeviceInfo(deviceType.name(), os, browser);
        } catch (Exception e) {
            LOG.warn("Failed to parse User-Agent: {}", e.getMessage());
            return DeviceInfo.unknown();
        }
    }

    /**
     * Accepts only the form factors a client may legitimately report about itself; anything
     * else is ignored so the header cannot inject arbitrary values into the logs.
     */
    private DeviceType parseDeviceTypeHint(String hint) {
        if (hint == null || hint.isBlank()) {
            return null;
        }
        try {
            DeviceType type = DeviceType.valueOf(hint.trim().toUpperCase(Locale.ROOT));
            return CLIENT_REPORTABLE_TYPES.contains(type) ? type : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Determines device type based on parsed client information.
     *
     * <p>The device family alone is not reliable: Chrome on Android sends a reduced
     * User-Agent with the model replaced by {@code "K"}, and most Android devices report a
     * bare model number such as {@code "SM-X710"}. On Android the presence of the
     * {@code Mobile} token is what separates phones from tablets.
     */
    private DeviceType determineDeviceType(Client client, String userAgent, String secChUaMobile) {
        String family = lower(client.device == null ? null : client.device.family);
        String os = lower(client.os == null ? null : client.os.family);
        String ua = userAgent.toLowerCase(Locale.ROOT);

        if (family.contains("spider") || family.contains("bot")) {
            return DeviceType.BOT;
        }
        if (isNativeHttpClient(ua)) {
            return DeviceType.MOBILE;
        }
        if (family.contains("ipad") || family.contains("tablet") || ua.contains("tablet")) {
            return DeviceType.TABLET;
        }
        if (os.contains("android")) {
            if ("?1".equals(secChUaMobile)) {
                return DeviceType.MOBILE;
            }
            return ua.contains("mobile") ? DeviceType.MOBILE : DeviceType.TABLET;
        }
        if (os.equals("ios") || family.contains("iphone") || family.contains("ipod")) {
            return DeviceType.MOBILE;
        }
        if (family.contains("tv") || ua.contains("smart-tv") || ua.contains("smarttv")) {
            return DeviceType.TV;
        }
        if (family.contains("phone") || ua.contains("mobile")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.DESKTOP;
    }

    /**
     * Checks if the user agent belongs to a native mobile HTTP client library rather than a
     * browser. Checked against the raw header because UAParser does not recognise these.
     */
    private boolean isNativeHttpClient(String ua) {
        return NATIVE_HTTP_CLIENT_MARKERS.stream().anyMatch(ua::contains);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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
            return new DeviceInfo(DeviceType.UNKNOWN.name(), "Unknown", "Unknown");
        }

        @Override
        public String toString() {
            return String.format("DeviceInfo{type=%s, os=%s, browser=%s}",
                    deviceType, operatingSystem, browser);
        }
    }
}
