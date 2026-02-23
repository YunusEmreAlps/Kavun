package com.kavun.web.payload.response;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class models the format of the response produced in the controller
 * endpoints.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
public class UserResponse implements Serializable {
  @Serial
  private static final long serialVersionUID = -8632756128923682589L;

  private Long id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private AuditInfo createdBy;
  private AuditInfo updatedBy;
  private AuditInfo deletedBy;
  private boolean deleted;
  private boolean enabled;
  private Set<AuditInfo> roles;

  /**
   * Inner class to represent audit information with id and name.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AuditInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
  }
}
