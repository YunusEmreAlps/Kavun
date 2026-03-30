package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.UserSession;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Specification for filtering UserSession entities.
 * Supports filtering by user, device, activity status, and date ranges.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserSessionSpecification extends BaseSpecification<UserSession> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Builds a dynamic specification based on search parameters.
     *
     * @param search Map containing filter parameters
     * @return Specification for UserSession
     */
    @SuppressWarnings("unchecked")
    public Specification<UserSession> search(Map<String, Object> search) {
        Specification<UserSession> specification = UserSessionSpecification.conjunction();

        // Filter by user ID
        if (search.containsKey("userId") && search.get("userId") != null) {
            String userIdStr = search.get("userId").toString();
            if (!userIdStr.isEmpty()) {
                UUID userId = UUID.fromString(userIdStr);
                specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("userId"), userId)
                );
            }
        }

        // Filter by device ID
        if (search.containsKey("deviceId") && search.get("deviceId") != null) {
            String deviceId = search.get("deviceId").toString();
            if (!deviceId.isEmpty()) {
                specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("deviceId"), deviceId)
                );
            }
        }

        // Filter by device type
        if (search.containsKey("deviceType") && search.get("deviceType") != null) {
            String deviceType = search.get("deviceType").toString();
            if (!deviceType.isEmpty()) {
                specification = specification.and(
                    (root, query, cb) -> cb.like(
                        cb.lower(root.get("deviceType")),
                        "%" + deviceType.toLowerCase() + "%"
                    )
                );
            }
        }

        // Filter by operating system
        if (search.containsKey("operatingSystem") && search.get("operatingSystem") != null) {
            String os = search.get("operatingSystem").toString();
            if (!os.isEmpty()) {
                specification = specification.and(
                    (root, query, cb) -> cb.like(
                        cb.lower(root.get("operatingSystem")),
                        "%" + os.toLowerCase() + "%"
                    )
                );
            }
        }

        // Filter by browser
        if (search.containsKey("browser") && search.get("browser") != null) {
            String browser = search.get("browser").toString();
            if (!browser.isEmpty()) {
                specification = specification.and(
                    (root, query, cb) -> cb.like(
                        cb.lower(root.get("browser")),
                        "%" + browser.toLowerCase() + "%"
                    )
                );
            }
        }

        // Filter by IP address
        if (search.containsKey("ipAddress") && search.get("ipAddress") != null) {
            String ipAddress = search.get("ipAddress").toString();
            if (!ipAddress.isEmpty()) {
                specification = specification.and(
                    (root, query, cb) -> cb.like(root.get("ipAddress"), ipAddress + "%")
                );
            }
        }

        // Filter by active status
        if (search.containsKey("isActive") && search.get("isActive") != null) {
            Boolean isActive = Boolean.parseBoolean(search.get("isActive").toString());
            specification = specification.and(
                (root, query, cb) -> cb.equal(root.get("isActive"), isActive)
            );
        }

        // Filter by logout type
        if (search.containsKey("logoutType") && search.get("logoutType") != null) {
            String logoutType = search.get("logoutType").toString();
            if (!logoutType.isEmpty()) {
                specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("logoutType"), logoutType)
                );
            }
        }

        // Filter by login date range (from)
        if (search.containsKey("loginAtFrom") && search.get("loginAtFrom") != null) {
            String dateStr = search.get("loginAtFrom").toString();
            if (!dateStr.isEmpty()) {
                LocalDateTime loginFrom = LocalDateTime.parse(dateStr);
                specification = specification.and(
                    (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("loginAt"), loginFrom)
                );
            }
        }

        // Filter by login date range (to)
        if (search.containsKey("loginAtTo") && search.get("loginAtTo") != null) {
            String dateStr = search.get("loginAtTo").toString();
            if (!dateStr.isEmpty()) {
                LocalDateTime loginTo = LocalDateTime.parse(dateStr);
                specification = specification.and(
                    (root, query, cb) -> cb.lessThanOrEqualTo(root.get("loginAt"), loginTo)
                );
            }
        }

        // Filter by last activity date range (from)
        if (search.containsKey("lastActivityFrom") && search.get("lastActivityFrom") != null) {
            String dateStr = search.get("lastActivityFrom").toString();
            if (!dateStr.isEmpty()) {
                LocalDateTime activityFrom = LocalDateTime.parse(dateStr);
                specification = specification.and(
                    (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("lastActivityAt"), activityFrom)
                );
            }
        }

        // Filter by last activity date range (to)
        if (search.containsKey("lastActivityTo") && search.get("lastActivityTo") != null) {
            String dateStr = search.get("lastActivityTo").toString();
            if (!dateStr.isEmpty()) {
                LocalDateTime activityTo = LocalDateTime.parse(dateStr);
                specification = specification.and(
                    (root, query, cb) -> cb.lessThanOrEqualTo(root.get("lastActivityAt"), activityTo)
                );
            }
        }

        return specification;
    }
}
