package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The user role model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "user_role", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_role", columnNames = { "user_id", "role_id" })
}, indexes = {
    @Index(name = "idx_user_role_user", columnList = "user_id"),
    @Index(name = "idx_user_role_role", columnList = "role_id"),
    @Index(name = "idx_user_role_deleted", columnList = "deleted"),
    @Index(name = "idx_user_role_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE user_role SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@ToString(callSuper = true, exclude = { "user", "role" })
public class UserRole extends BaseEntity<Long> implements Serializable {
  @Serial
  private static final long serialVersionUID = 2803657434288286128L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @NotNull(message = "User cannot be null")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false)
  @NotNull(message = "Role cannot be null")
  private Role role;

  /**
   * Constructor for UserRole.
   *
   * @param user user for object instantiation.
   * @param role user for object instantiation.
   */
  public UserRole(User user, Role role) {
    this.setPublicId(UUID.randomUUID().toString());
    this.user = user;
    this.role = role;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof UserRole that) || !super.equals(o))
      return false;

    return Objects.equals(
        user != null ? user.getId() : null,
        that.user != null ? that.user.getId() : null)
        && Objects.equals(
            role != null ? role.getId() : null,
            that.role != null ? that.role.getId() : null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        user != null ? user.getId() : null,
        role != null ? role.getId() : null);
  }

  protected boolean canEqual(Object other) {
    return other instanceof UserRole;
  }
}
