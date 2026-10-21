package com.kavun.shared.dto;

import com.kavun.constant.email.EmailConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The EmailDto transfers email details from outside into the application and vice versa.
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
public class EmailDto extends BaseDto {

  @Serial private static final long serialVersionUID = -6342630857637389028L;

  @Size(max = EmailConstants.TITLE_MAX_SIZE, message = EmailConstants.TITLE_SIZE)
  private String title;

  @Size(max = EmailConstants.MAIL_MAX_SIZE, message = EmailConstants.MAIL_SIZE)
  @Email(message = EmailConstants.INVALID_EMAIL)
  private String mail;

  private String message;
  private String result;
  private String requestId;

  private boolean bodyHtml;
  private boolean status;
}
