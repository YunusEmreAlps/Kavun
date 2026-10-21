package com.kavun.web.payload.request;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {
  @NotBlank private String token;

  @NotBlank private String newPassword;

  // Getters and setters
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
