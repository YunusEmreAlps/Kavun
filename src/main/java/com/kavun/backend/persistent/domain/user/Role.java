package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.RoleType;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLDelete;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE roles SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Role extends BaseEntity<Long> implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Column(nullable = false, length = 100, unique = true)
  @ToString.Include
  private String name;

  @Column(length = 100)
  private String label;

  @Column(length = 500)
  private String description;

  // System Role
  @Column(name = "is_system_role", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean systemRole;

  public Role(RoleType roleType) {
    // this.id = roleType.getId();
    this.setPublicId(UUID.randomUUID().toString());
    this.name = roleType.getName();
    this.description = roleType.getDescription();
    this.systemRole = roleType.isSystemRole();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Role that))
      return false;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
