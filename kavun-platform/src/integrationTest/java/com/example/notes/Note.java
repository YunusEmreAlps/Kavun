package com.example.notes;

import com.kavun.backend.persistent.domain.base.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "notes")
@Getter
@Setter
@SQLDelete(
    sql = "UPDATE notes SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Note extends TenantScopedEntity {

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;
}
