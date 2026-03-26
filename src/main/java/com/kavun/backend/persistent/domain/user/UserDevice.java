package com.kavun.backend.persistent.domain.user;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

/**
 * The user devices model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 *
 */
@Entity
@Table(name = "user_devices")
@Getter
@Setter
@SQLDelete(sql = "UPDATE user_devices SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserDevice extends BaseEntity<Long> implements Serializable {

    @Column(nullable = false)
    private Long userId;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
}
