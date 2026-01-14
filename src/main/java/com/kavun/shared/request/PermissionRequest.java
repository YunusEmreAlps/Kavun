package com.kavun.shared.request;

import java.time.LocalDateTime;

import com.kavun.enums.EntityType;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionRequest extends BaseRequest {
    private EntityType entityType;
    private Long entityId; // ID of the Role or User
    private Long pageActionId;
    private boolean granted;
    private LocalDateTime expiresAt;
}
