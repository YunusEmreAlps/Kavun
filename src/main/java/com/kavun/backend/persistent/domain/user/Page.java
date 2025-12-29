package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import java.io.Serializable;

import org.hibernate.annotations.SQLDelete;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * The page model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 *
 */
@Entity
@Table(name = "pages",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
    },
    indexes = {
        @Index(name = "idx_page_parent", columnList = "parent_id"),
        @Index(name = "idx_page_code", columnList = "code"),
        @Index(name = "idx_page_order", columnList = "display_order"),
        @Index(name = "idx_page_deleted", columnList = "deleted")
    }
)
@Getter @Setter
@SQLDelete(sql = "UPDATE pages SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Page extends BaseEntity<Long> implements Serializable {

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Page parent;

    @Column(nullable = false)
    private Integer level = 0;

    // Calculate level before persisting or updating
    @PrePersist
    @PreUpdate
    private void calculateLevel() {
        this.level = parent == null ? 0 : parent.getLevel() + 1;
    }
}
