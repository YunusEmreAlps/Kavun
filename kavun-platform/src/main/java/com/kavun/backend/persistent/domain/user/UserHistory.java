package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.UserHistoryType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Class UserHistory captures activities happening to user such as profile update, password reset,
 * etc.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
@Entity
@NoArgsConstructor
@ToString(callSuper = true)
public class UserHistory extends BaseEntity<Long> implements Serializable {
  @Serial private static final long serialVersionUID = -418682848586685969L;

  @Setter
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = User.class)
  private User user;

  @Enumerated(EnumType.ORDINAL)
  private UserHistoryType userHistoryType;

  /**
   * Constructor for UserHistory.
   *
   * @param publicId the publicId
   * @param user the user
   * @param userHistoryType the userHistoryType
   */
  public UserHistory(String publicId, User user, UserHistoryType userHistoryType) {
    this.setPublicId(publicId);
    this.user = user;
    this.userHistoryType = userHistoryType;
  }
}
