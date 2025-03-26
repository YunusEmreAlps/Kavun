package com.kavun.web.payload.request.mail;

import com.kavun.shared.dto.UserDto;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thymeleaf.context.Context;

/**
 * The pojo used to conveniently transport email details to the java mail sender.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class HtmlEmailRequest extends EmailRequest {
  private String template;
  private UserDto sender;
  private UserDto receiver;
  private Context context;

  private Set<File> attachments = new HashSet<>();
}
