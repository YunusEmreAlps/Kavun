package com.kavun.backend.service.health;

import com.kavun.backend.service.security.ClamAVService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for ClamAV virus scanner service.
 * Reports ClamAV availability and version in actuator health endpoint.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClamAVHealthIndicator implements HealthIndicator {

    private final ClamAVService clamAVService;

    @Override
    public Health health() {
        try {
            boolean isAvailable = clamAVService.isAvailable();

            if (isAvailable) {
                String version = clamAVService.getVersion();
                return Health.up()
                        .withDetail("version", version)
                        .withDetail("available", true)
                        .build();
            } else {
                return Health.down()
                        .withDetail("available", false)
                        .withDetail("reason", "ClamAV service is not responding")
                        .build();
            }
        } catch (Exception e) {
            LOG.error("Error checking ClamAV health: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("available", false)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
