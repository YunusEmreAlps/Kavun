package com.kavun.shared.dto;

import com.kavun.enums.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The ActionDto transfers action details from outside into the application and vice versa.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ActionDto extends BaseDto {

  @Serial private static final long serialVersionUID = 1L;

  @NotBlank(message = "Action code cannot be blank")
  private String code;

  @NotBlank(message = "Action name cannot be blank")
  private String name;

  @NotNull(message = "Action type cannot be null")
  private ActionType type;
}
