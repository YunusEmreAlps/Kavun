package com.kavun.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApi configuration properties groups all properties prefixed with "openapi.".
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

  private String name;
  private String version;
  private String description;
}
