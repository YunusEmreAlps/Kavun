package com.kavun.shared.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The PageDto transfers page details from outside into the application and vice versa.
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
public class PageDto extends BaseDto {

  @Serial private static final long serialVersionUID = 1L;

  @NotBlank(message = "Page code cannot be blank")
  private String code;

  @NotBlank(message = "Page name cannot be blank")
  private String name;

  private String url;
  private String description;
  private String icon;
  private Long parentId;
}
