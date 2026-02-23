package com.kavun.backend.persistent.domain.upload;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.enums.EntityType;
import com.kavun.enums.FileType;
import com.kavun.enums.UploadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Index;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * Stores metadata about uploaded files, such as their location in the storage service, content type, size, and checksum for integrity verification.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(
    name = "file_metadata",
    indexes = {
        @Index(name = "idx_file_created_by", columnList = "created_by"),
        @Index(name = "idx_file_object_key", columnList = "object_key"),
        @Index(name = "idx_file_checksum", columnList = "checksum"),
        @Index(name = "idx_file_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_file_type", columnList = "file_type"),
        @Index(name = "idx_file_status", columnList = "upload_status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_object_key", columnNames = {"object_key"})
    }
)
@Getter @Setter
public class FileMetadata extends BaseEntity<Long> implements Serializable {
    // File type classification
    @NotNull(message = "File type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 50)
    private FileType fileType;

    // Entity association - polymorphic reference
    @NotNull(message = "Entity type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;

    @NotNull(message = "Entity ID is required")
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @NotBlank(message = "Bucket name is required")
    @Size(max = 255, message = "Bucket name cannot exceed 255 characters")
    @Column(nullable = false)
    private String bucket;

    @NotBlank(message = "Object key is required")
    @Size(max = 512, message = "Object key cannot exceed 512 characters")
    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Size(max = 100, message = "Content type cannot exceed 100 characters")
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    @Column(nullable = false)
    private Long size;

    @NotBlank(message = "Checksum is required")
    @Size(max = 64, message = "Checksum cannot exceed 64 characters")
    @Column(nullable = false)
    private String checksum; // SHA-256 hash for integrity verification

    // Upload status tracking
    @NotNull(message = "Upload status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private UploadStatus uploadStatus = UploadStatus.COMPLETED;

    // Access control
    @Column(name = "access_url", length = 1024)
    private String accessUrl; // Pre-signed URL or public URL

    @Column(name = "access_url_expires_at")
    private LocalDateTime accessUrlExpiresAt;

    // File lifecycle management
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;

    // Additional metadata
    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    // Error tracking
    @Column(name = "error_message", length = 1000)
    private String errorMessage; // If upload failed

    /**
     * Helper method to check if access URL is expired
     */
    public boolean isAccessUrlExpired() {
        return accessUrlExpiresAt != null && LocalDateTime.now().isAfter(accessUrlExpiresAt);
    }

    /**
     * Increment access count
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
}
