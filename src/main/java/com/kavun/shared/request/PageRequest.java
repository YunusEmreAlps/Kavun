package com.kavun.shared.request;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class PageRequest extends BaseRequest {
    private String code;
    private String name;
    private String url;
    private String description;
    private String icon;
    private Integer displayOrder;
    private Long parentId;
}
