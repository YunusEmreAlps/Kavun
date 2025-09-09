package com.kavun.web.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for initiating a password reset.
 */
@Getter
@Setter
public class ForgotPasswordRequest {

  @Email(message = "Please provide a valid email address")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  @NotBlank(message = "Email cannot be blank")
  private String email;

  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;
}
