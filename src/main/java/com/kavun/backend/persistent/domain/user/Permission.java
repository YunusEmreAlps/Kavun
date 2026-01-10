package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.EntityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * The permission model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 *
 */
@Entity
@Table(name = "permissions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type", "entity_id", "page_action_id"})
    },
    indexes = {
        @Index(name = "idx_permission_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_permission_page_action", columnList = "page_action_id"),
        @Index(name = "idx_permission_expires", columnList = "expires_at"),
        @Index(name = "idx_permission_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@SQLDelete(sql = "UPDATE permissions SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Permission extends BaseEntity<Long> implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column(nullable = false)
    private Long entityId; // ID of the Role or User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_action_id", nullable = false)
    private PageAction pageAction;

    @Column(nullable = false)
    private boolean granted = true;

    private LocalDateTime expiresAt;

    // Composite unique constraint validation
    @PrePersist
    @PreUpdate
    private void validateUnique() {
        // This method can be used to enforce unique constraints programmatically if
        // needed
    }

    /**
     * Check if the permission is valid (not expired).
     *
     * @return true if the permission is valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Check if the permission is expired.
     *
     * @return true if the permission is expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
