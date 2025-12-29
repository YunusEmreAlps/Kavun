package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.ActionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;

import org.hibernate.annotations.SQLDelete;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * The action model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 *
 */
@Entity
@Table(name = "actions", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code")
})
@Getter @Setter
@SQLDelete(sql = "UPDATE actions SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Action extends BaseEntity <Long> implements Serializable {

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;
}
