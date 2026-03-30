package com.kavun.shared.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDeviceDto extends BaseDto {
    private String deviceId;
    private String deviceType;
    private String operatingSystem;
    private String browser;
}
