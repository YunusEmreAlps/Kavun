package com.kavun.shared.dto;

import java.io.Serial;
import java.io.Serializable;

import com.kavun.constant.email.EmailConstants;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class EmailDto extends BaseDto implements Serializable {
  @Serial private static final long serialVersionUID = -6342630857637389028L;

  @Size(max = EmailConstants.TITLE_MAX_SIZE, message = EmailConstants.TITLE_SIZE)
  private String title;

  @Size(max = EmailConstants.MAIL_MAX_SIZE, message = EmailConstants.MAIL_SIZE)
  @Email(message = EmailConstants.INVALID_EMAIL)
  private String mail;

  private String message;
  private String result;
  private String requestId;

  private boolean isBodyHtml;
  private boolean status;
}
