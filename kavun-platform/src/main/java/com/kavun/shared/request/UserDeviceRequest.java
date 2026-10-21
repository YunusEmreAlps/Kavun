package com.kavun.shared.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDeviceRequest extends BaseRequest {
    private String deviceId;
    private String deviceType;
    private String operatingSystem;
    private String browser;
}
