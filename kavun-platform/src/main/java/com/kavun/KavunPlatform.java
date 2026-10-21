package com.kavun;

import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The Kavun platform. An application built on it imports this class next to its own
 * {@code @SpringBootApplication}:
 *
 * <pre>{@code
 * @SpringBootApplication
 * @Import(KavunPlatform.class)
 * public class JewelerApplication {
 *   public static void main(String[] args) {
 *     SpringApplication.run(JewelerApplication.class, args);
 *   }
 * }
 * }</pre>
 *
 * <p>The platform's components, entities and repositories are then found next to the
 * application's own, which stay in the application's package as usual. Deliberately not a
 * {@code @Configuration}, so that scanning the platform does not register it a second time.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@EnableScheduling
@AutoConfigurationPackage
@ComponentScan(
    excludeFilters = {
      @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
    })
public class KavunPlatform {}
