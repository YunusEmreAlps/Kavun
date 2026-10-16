package com.kavun.shared.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Creates or updates a tenant. The code and the first administrator are only used on creation;
 * the code never changes afterwards.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = "adminPassword")
@EqualsAndHashCode(callSuper = true)
public class TenantRequest extends BaseRequest {

  @Serial private static final long serialVersionUID = 4087315620912766331L;

  /** Validation group of the fields required when a tenant is created. */
  public interface Create {}

  @NotBlank(groups = Create.class)
  @Size(max = 50)
  @Pattern(
      regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
      message = "Tenant code may only contain lowercase letters, digits and single hyphens")
  private String code;

  @NotBlank
  @Size(max = 200)
  private String name;

  private Boolean enabled;

  @NotBlank(groups = Create.class)
  @Size(min = 3, max = 50, groups = Create.class)
  private String adminUsername;

  @NotBlank(groups = Create.class)
  @Email(groups = Create.class)
  private String adminEmail;

  @NotBlank(groups = Create.class)
  @Size(min = 8, max = 100, groups = Create.class)
  private String adminPassword;
}
