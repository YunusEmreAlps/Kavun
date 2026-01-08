package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.RoleType;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The role entity.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Getter
@Setter
@ToString
@Cacheable
@NoArgsConstructor
public class Role extends BaseEntity<Long> implements Serializable {
  @Serial private static final long serialVersionUID = 7008351760784988067L;

  // @Id private Integer id;
  private String name;
  private String description;

  /**
   * The Role class creates a role for the user.
   *
   * @param roleType assigns the role properties.
   */
  public Role(RoleType roleType) {
    // this.id = roleType.getId();
    this.setPublicId(UUID.randomUUID().toString());
    this.name = roleType.getName();
    this.description = roleType.getDescription();
  }

  /**
   * Evaluate the equality of Role class.
   *
   * @param other is the object to use in equality test.
   * @return the equality of both objects.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    return (other instanceof Role that) && Objects.equals(name, that.name);
  }

  /**
   * Hashcode of Role base on name.
   *
   * @return the hash value.
   */
  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
