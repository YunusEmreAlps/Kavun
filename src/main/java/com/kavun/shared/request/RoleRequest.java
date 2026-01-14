package com.kavun.shared.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * The RoleRequest transfers role details from outside into the application and vice versa.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RoleRequest extends BaseRequest {

  @Serial private static final long serialVersionUID = -6342630857637389028L;

  private String name;
  private String description;
  private String label;
}
