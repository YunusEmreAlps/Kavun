package com.kavun.web.payload.request;

import com.kavun.constant.user.UserConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.ToString;

/**
 * Request payload for the authenticated user's password update.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
public class UpdatePasswordRequest {

  @ToString.Exclude
  @NotBlank(message = UserConstants.BLANK_PASSWORD)
  private String oldPassword;

  @ToString.Exclude
  @NotBlank(message = UserConstants.BLANK_PASSWORD)
  @Size(min = UserConstants.PASSWORD_MIN_SIZE, max = UserConstants.PASSWORD_MAX_SIZE, message = UserConstants.PASSWORD_SIZE)
  private String newPassword;
}
