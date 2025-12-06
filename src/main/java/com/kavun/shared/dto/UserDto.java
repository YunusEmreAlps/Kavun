package com.kavun.shared.dto;

import com.kavun.backend.persistent.domain.user.UserHistory;
import com.kavun.backend.persistent.domain.user.UserRole;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.OtpDeliveryMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

/**
 * The UserDto transfers user details from outside into the application and vice versa.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserDto extends BaseDto {

  @Serial private static final long serialVersionUID = -6342630857637389028L;

  @NotBlank(message = UserConstants.BLANK_USERNAME)
  private String username;

  @ToString.Exclude
  @NotBlank(message = UserConstants.BLANK_PASSWORD)
  private String password;

  private String firstName;
  private String middleName;
  private String lastName;

  @NotBlank(message = UserConstants.BLANK_EMAIL)
  @Email(message = UserConstants.INVALID_EMAIL)
  private String email;

  private String role;
  private String phone;
  private String profileImage;
  private String verificationToken;

  private int failedLoginAttempts;
  private LocalDateTime lastSuccessfulLogin;

  private boolean enabled;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;

  /** OTP delivery method, defaults to EMAIL. */
  @lombok.Builder.Default
  private String otpDeliveryMethod = OtpDeliveryMethod.EMAIL.name();

  @ToString.Exclude
  @lombok.Builder.Default
  private Set<UserRole> userRoles = new HashSet<>();

  @ToString.Exclude
  @lombok.Builder.Default
  private Set<UserHistory> userHistories = new HashSet<>();

  /**
   * Formulates the full name of the user.
   *
   * @return the full name of the user
   */
  public String getName() {
    return StringUtils.joinWith(StringUtils.SPACE, getFirstName(), getMiddleName(), getLastName());
  }
}
