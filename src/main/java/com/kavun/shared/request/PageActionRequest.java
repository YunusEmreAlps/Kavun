package com.kavun.shared.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class PageActionRequest extends BaseRequest {
    @NotNull(message = "Page ID cannot be null")
    private Long pageId;

    @NotNull(message = "Action ID cannot be null")
    private Long actionId;

    private String apiEndpoint;  // Optional
    private String httpMethod;    // Optional

    @NotBlank(message = "Label cannot be blank")
    private String label;
}
