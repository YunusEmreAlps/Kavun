package com.kavun.shared.request;

import com.kavun.constant.user.UserConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

  @NotBlank(message = UserConstants.BLANK_NAME)
  @Size(min = UserConstants.ROLE_NAME_MIN_SIZE, max = UserConstants.ROLE_NAME_MAX_SIZE,
      message = UserConstants.ROLE_NAME_SIZE_MESSAGE)
  private String name;

  @Size(max = UserConstants.ROLE_DESCRIPTION_MAX_SIZE, message = UserConstants.ROLE_DESCRIPTION_SIZE_MESSAGE)
  private String description;

  @Size(max = UserConstants.ROLE_LABEL_MAX_SIZE, message = UserConstants.ROLE_LABEL_SIZE_MESSAGE)
  private String label;
}
