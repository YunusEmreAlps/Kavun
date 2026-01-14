package com.kavun.shared.request;


import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class PageActionRequest extends BaseRequest {
    private Long pageId;
    private Long actionId;
    private String apiEndpoint;
    private String httpMethod;
    private String label;
}
