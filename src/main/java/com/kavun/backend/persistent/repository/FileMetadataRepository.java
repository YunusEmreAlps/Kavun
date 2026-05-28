package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.upload.FileMetadata;
import com.kavun.enums.EntityType;
import com.kavun.enums.FileType;
import com.kavun.enums.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link FileMetadata} entity.
 *æ
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface FileMetadataRepository extends BaseRepository<FileMetadata> {

    // Find file by object key
    Optional<FileMetadata> findByObjectKey(String objectKey);

    // Find all files by entity type (for category-based types like LIBRARY)
    List<FileMetadata> findByEntityType(EntityType entityType);

    // Find all files by entity type and entity id (for actual entities like USER, COMPANY)
    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, UUID entityId);

    // Find files by entity type with optional entity id
    @Query("SELECT f FROM FileMetadata f WHERE f.entityType = :entityType " +
           "AND (:entityId IS NULL OR f.entityId = :entityId)")
    List<FileMetadata> findByEntityTypeAndOptionalEntityId(
            @Param("entityType") EntityType entityType,
            @Param("entityId") UUID entityId
    );

    // Find category files (entityId is null)
    @Query("SELECT f FROM FileMetadata f WHERE f.entityType = :entityType AND f.entityId IS NULL")
    List<FileMetadata> findByEntityTypeWithoutEntityId(@Param("entityType") EntityType entityType);

    // Find all files by entity type, entity id and file type
    List<FileMetadata> findByEntityTypeAndEntityIdAndFileType(
            EntityType entityType,
            UUID entityId,
            FileType fileType
    );

    // Find files by entity type and file type (for category-based types)
    List<FileMetadata> findByEntityTypeAndFileType(
            EntityType entityType,
            FileType fileType
    );

    // Find files by file type
    Page<FileMetadata> findByFileType(FileType fileType, Pageable pageable);

    // Find files by upload status
    List<FileMetadata> findByUploadStatus(UploadStatus status);

    // Find expired files
    @Query("SELECT f FROM FileMetadata f WHERE f.accessUrlExpiresAt < :now AND f.accessUrlExpiresAt IS NOT NULL")
    List<FileMetadata> findExpiredAccessUrls(@Param("now") LocalDateTime now);

    // Find files by checksum
    Optional<FileMetadata> findByChecksum(String checksum);

    // Check if file exists by object key
    boolean existsByObjectKey(String objectKey);

    // Delete by object key
    void deleteByObjectKey(String objectKey);

    // Find files by entity and created by user
    @Query("SELECT f FROM FileMetadata f WHERE f.entityType = :entityType " +
           "AND (:entityId IS NULL OR f.entityId = :entityId) AND f.createdBy = :userId")
    List<FileMetadata> findByEntityAndCreatedBy(
            @Param("entityType") EntityType entityType,
            @Param("entityId") UUID entityId,
            @Param("userId") UUID userId
    );

    // Count files by entity type (for categories without entityId)
    Long countByEntityType(EntityType entityType);

    // Count files by entity type and entity id (for actual entities)
    Long countByEntityTypeAndEntityId(EntityType entityType, UUID entityId);
}
