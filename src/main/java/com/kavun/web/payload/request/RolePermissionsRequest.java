package com.kavun.web.payload.request;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * The checked cells of a role's permission matrix: every page action the role should be granted.
 *
 * @param pageActionIds the granted page actions
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public record RolePermissionsRequest(
    @NotNull(message = "Page action IDs are required") Set<Long> pageActionIds) {}
