package com.kavun.shared.dto;

import com.kavun.enums.EntityType;

import java.time.LocalDateTime;

/**
 * Lightweight projection of a permission record, used to resolve all permissions of a user in a
 * single query without loading {@code Permission} entities.
 *
 * @param pageActionId the page action the permission applies to
 * @param entityType   whether the permission is assigned to the user or to one of its roles
 * @param granted      true for allow, false for an explicit deny
 * @param expiresAt    optional expiry time, null if the permission never expires
 */
public record PermissionGrantDto(
    Long pageActionId, EntityType entityType, boolean granted, LocalDateTime expiresAt) {

  /**
   * Check if the permission is expired at the given time.
   *
   * @param now the reference time
   * @return true if the permission is expired, false otherwise
   */
  public boolean isExpired(LocalDateTime now) {
    return expiresAt != null && now.isAfter(expiresAt);
  }
}
