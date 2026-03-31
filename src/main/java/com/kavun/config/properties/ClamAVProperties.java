package com.kavun.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for ClamAV virus scanner integration.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "clamav")
public class ClamAVProperties {

    private boolean enabled;
    private String host;
    private int port;
    private int timeout;
    private boolean asyncScan;
}
