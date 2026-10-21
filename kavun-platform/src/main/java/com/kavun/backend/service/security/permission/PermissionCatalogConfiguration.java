package com.kavun.backend.service.security.permission;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the {@link PermissionCatalog} from every registered contributor.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class PermissionCatalogConfiguration {

  /**
   * The catalog of all modules; startup fails if the declarations are inconsistent.
   *
   * @param contributors the modules' contributors
   * @return the catalog
   */
  @Bean
  public PermissionCatalog permissionCatalog(List<PermissionCatalogContributor> contributors) {
    var builder = PermissionCatalog.builder();
    contributors.forEach(contributor -> contributor.contribute(builder));
    return builder.build();
  }
}
