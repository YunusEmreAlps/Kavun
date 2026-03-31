package com.kavun.web.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaResponse {
  private String imageBase64;
  private String captchaId;
}
