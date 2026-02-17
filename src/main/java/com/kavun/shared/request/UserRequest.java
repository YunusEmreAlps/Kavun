package com.kavun.shared.request;

import com.kavun.constant.AuthConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.web.payload.request.UserRoleRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

/**
 * User request DTO for creating and updating users.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserRequest extends BaseRequest {

  @EqualsAndHashCode.Include
  @NotBlank(message = UserConstants.BLANK_USERNAME)
  @Size(min = UserConstants.USERNAME_MIN_SIZE, max = UserConstants.USERNAME_MAX_SIZE, message = UserConstants.USERNAME_SIZE)
  private String username;

  @Size(max = 60)
  @EqualsAndHashCode.Include
  @Email(message = UserConstants.INVALID_EMAIL)
  @NotBlank(message = UserConstants.BLANK_EMAIL)
  private String email;

  @ToString.Exclude
  @Size(min = UserConstants.PASSWORD_MIN_SIZE, max = UserConstants.PASSWORD_MAX_SIZE, message = UserConstants.PASSWORD_SIZE)
  private String password; // Optional for updates, required for creates

  @Size(min = UserConstants.FIRSTNAME_MIN_SIZE, max = UserConstants.FIRSTNAME_MAX_SIZE, message = UserConstants.NAME_SIZE)
  @NotBlank(message = UserConstants.BLANK_FIRST_NAME)
  private String firstName;

  @Size(min = UserConstants.LASTNAME_MIN_SIZE, max = UserConstants.LASTNAME_MAX_SIZE, message = UserConstants.NAME_SIZE)
  @NotBlank(message = UserConstants.BLANK_LAST_NAME)
  private String lastName;

  private String phone;
  private String profileImage;

  private boolean enabled;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;

  @NotBlank(message = AuthConstants.BLANK_OTP_DELIVERY_METHOD)
  private String otpDeliveryMethod;

  @Valid
  @Builder.Default
  private ArrayList<UserRoleRequest> roles = new ArrayList<>();
}
