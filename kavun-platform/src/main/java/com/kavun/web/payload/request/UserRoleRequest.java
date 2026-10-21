package com.kavun.web.payload.request;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple structure for userRoleRequest: { "roleId": 5 }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRequest implements Serializable {
    @NotNull(message = "Role ID is required")
    private Long roleId;
}
