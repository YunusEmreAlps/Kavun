package com.kavun.web.payload.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * Request payload for generating an OTP for a user.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
public class GenerateOtpRequest {

  @NotBlank(message = "Username cannot be blank")
  private String username;
}
