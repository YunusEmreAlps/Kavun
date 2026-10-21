package com.kavun.shared.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ActionRequest extends BaseRequest {
    private String code;
    private String name;
    private String type;
}
