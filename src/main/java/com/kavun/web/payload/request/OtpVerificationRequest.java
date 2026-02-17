package com.kavun.web.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for OTP verification.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationRequest {

  @NotNull(message = "OTP ID cannot be null")
  private Long id;

  @NotBlank(message = "Target (email or phone) cannot be blank")
  private String target;

  @NotBlank(message = "OTP code cannot be blank")
  @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
  private String code;
}
