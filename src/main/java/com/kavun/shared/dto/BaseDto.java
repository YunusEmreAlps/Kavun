package com.kavun.shared.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kavun.backend.serializer.UserInfoObjectSerializer;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The BaseDto provides base fields to be extended by all DTOs.
 *
 * <p>This class contains common audit fields that map to {@link
 * com.kavun.backend.persistent.domain.base.BaseEntity}.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseDto implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Internal database identifier. Should NOT be exposed in public APIs. Use {@link #publicId} for
   * external communication.
   */
  private Long id;

  /** The unique public identifier exposed to external systems. */
  @EqualsAndHashCode.Include
  private String publicId;

  /** The optimistic locking version. */
  @EqualsAndHashCode.Include
  private int version;

  /** Timestamp when the entity was created. */
  private LocalDateTime createdAt;

  /** Username of the creator. */
  @JsonSerialize(using = UserInfoObjectSerializer.class)
  private Long createdBy;

  /** Timestamp when the entity was last updated. */
  private LocalDateTime updatedAt;

  /** Username of the last updater. */
  @JsonSerialize(using = UserInfoObjectSerializer.class)
  private Long updatedBy;

  @Builder.Default
  private boolean deleted = false;

  private LocalDateTime deletedAt;

  @JsonSerialize(using = UserInfoObjectSerializer.class)
  private Long deletedBy;
}
