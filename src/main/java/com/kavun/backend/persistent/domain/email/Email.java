package com.kavun.backend.persistent.domain.email;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.constant.email.EmailConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;

/**
 * The Email model for the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Email extends BaseEntity<Long> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column
    @Size(max = com.kavun.constant.email.EmailConstants.TITLE_MAX_SIZE, message = EmailConstants.TITLE_SIZE)
    private String title;

    @Column
    @Size(max = EmailConstants.MAIL_MAX_SIZE, message = EmailConstants.MAIL_SIZE)
    private String mail;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(nullable = false)
    private boolean isBodyHtml = false;

    @Column(nullable = false)
    private boolean status = false;
}
