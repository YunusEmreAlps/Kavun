package com.kavun.shared.dto;

import com.kavun.enums.UserHistoryType;
import com.kavun.web.payload.pojo.SeparateDateFormat;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The UserHistoryDto transfers user history from outside into the application and vice versa.
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
public final class UserHistoryDto extends BaseDto {

  @Serial private static final long serialVersionUID = -8842211126703873453L;

  private UserHistoryType userHistoryType;
  private String timeElapsedDescription;
  private SeparateDateFormat separateDateFormat;
}
