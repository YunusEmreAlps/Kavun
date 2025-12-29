package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.HttpMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import java.io.Serializable;

import org.hibernate.annotations.SQLDelete;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * The page action model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 *
 */
@Entity
@Table(name = "page_actions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"page_id", "action_id"})
    },
    indexes = {
        @Index(name = "idx_page_action_page", columnList = "page_id"),
        @Index(name = "idx_page_action_action", columnList = "action_id"),
        @Index(name = "idx_page_action_deleted", columnList = "deleted")
    }
)
@Getter @Setter
@SQLDelete(sql = "UPDATE page_actions SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PageAction extends BaseEntity<Long> implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    private HttpMethod httpMethod;

    @Column(nullable = false, length = 200)
    private String label;

    // Helper method to check if the action is of type VIEW
    public boolean isViewAction() {
        return "VIEW".equals(action.getCode());
    }
}
