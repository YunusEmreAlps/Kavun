package com.kavun.shared.dto;

import java.io.Serial;
import java.time.LocalDateTime;

import com.kavun.enums.EntityType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The PermissionDto transfers permission details from outside into the application and vice versa.
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
public class PermissionDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    private EntityType entityType;
    private Long entityId; // ID of the Role or User
    private Long pageActionId;
    private boolean granted;
    private LocalDateTime expiresAt;
}
