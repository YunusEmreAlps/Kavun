package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.TenantScopedEntity;
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
    @Index(name = "idx_user_role_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE user_role SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@ToString(callSuper = true, exclude = { "user", "role" })
public class UserRole extends TenantScopedEntity implements Serializable {
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
    this.user = user;
    this.role = role;
    setTenantCode(user.getTenantCode());
  }
}
