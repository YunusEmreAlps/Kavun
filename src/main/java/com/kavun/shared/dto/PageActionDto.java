package com.kavun.shared.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The PageActionDto transfers action details from outside into the application and
 * vice versa.
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
public class PageActionDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Page ID cannot be blank")
    private Long pageId;

    @NotBlank(message = "Action ID cannot be blank")
    private Long actionId;

    private String apiEndpoint;
    private String httpMethod;
    private String label;
}
