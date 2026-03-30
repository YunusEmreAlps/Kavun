package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.UserDevice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Specification for filtering UserDevice entities.
 * Supports filtering by user, device, activity status, and date ranges.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserDeviceSpecification extends BaseSpecification<UserDevice> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Builds a dynamic specification based on search parameters.
     *
     * @param search Map containing filter parameters
     * @return Specification for UserDevice
     */
    @SuppressWarnings("unchecked")
    public Specification<UserDevice> search(Map<String, Object> search) {
        Specification<UserDevice> specification = UserDeviceSpecification.conjunction();

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

        return specification;
    }
}
