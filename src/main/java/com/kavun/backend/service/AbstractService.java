package com.kavun.backend.service;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.backend.persistent.repository.BaseRepository;
import com.kavun.backend.persistent.specification.BaseSpecification;
import com.kavun.backend.service.base.BaseService;
import com.kavun.shared.dto.BaseDto;
import com.kavun.shared.dto.mapper.BaseMapper;
import com.kavun.shared.request.BaseRequest;
import com.kavun.shared.util.core.SecurityUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base service implementation providing common CRUD operations.
 * <p>
 * This class implements the Template Method pattern, allowing subclasses to customize
 * behavior through protected hook methods while maintaining consistent operation flow.
 * </p>
 *
 * @param <REQUEST> Request type for create/update operations
 * @param <ENTITY> Entity type extending BaseEntity
 * @param <DTO> Data Transfer Object type for responses
 * @param <REPO> Repository interface extending BaseRepository
 * @param <MAPPER> Mapper interface for entity/dto conversions
 * @param <SPEC> Specification builder for queries
 */
@Slf4j
public abstract class AbstractService<REQUEST extends BaseRequest, ENTITY extends BaseEntity<Long>, DTO extends BaseDto, REPO extends BaseRepository<ENTITY>, MAPPER extends BaseMapper<REQUEST, ENTITY, DTO>, SPEC extends BaseSpecification<ENTITY>>
        implements BaseService<REQUEST, DTO, ENTITY> {

    protected final MAPPER mapper;
    protected final REPO repository;
    protected final SPEC specification;

    protected AbstractService(MAPPER mapper, REPO repository, SPEC specification) {
        this.mapper = Objects.requireNonNull(mapper, "Mapper cannot be null");
        this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
        this.specification = Objects.requireNonNull(specification, "Specification cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public DTO findById(Long id) {
        LOG.debug("Finding entity by id: {}", id);
        ENTITY entity = getEntityById(id);
        return mapper.toDto(entity);
    }

    @Override
    @Transactional
    public DTO create(REQUEST request) {
        validateCreateRequest(request);
        LOG.debug("Creating new entity from request: {}", request);

        ENTITY entity = mapToEntity(request);
        beforeCreate(entity);
        ENTITY savedEntity = repository.save(entity);
        afterCreate(savedEntity);

        LOG.info("Entity created successfully with id: {}", savedEntity.getId());
        return mapper.toDto(savedEntity);
    }

    @Override
    @Transactional
    public DTO update(Long id, REQUEST request) {
        validateUpdateRequest(request);
        LOG.debug("Updating entity with id: {}", id);

        ENTITY entity = getEntityById(id);
        ensureNotDeleted(entity);

        updateEntity(entity, request);
        beforeUpdate(entity);
        ENTITY updatedEntity = repository.save(entity);
        afterUpdate(updatedEntity);

        LOG.info("Entity updated successfully with id: {}", id);
        return mapper.toDto(updatedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LOG.debug("Soft deleting entity with id: {}", id);

        ENTITY entity = getEntityById(id);

        if (entity.isDeleted()) {
            LOG.warn("Entity with id {} is already deleted", id);
            throw new IllegalStateException("Kayıt zaten silinmiş durumda");
        }

        beforeDelete(entity);
        applySoftDelete(entity);
        repository.save(entity);
        afterDelete(entity);

        LOG.info("Entity soft deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public DTO restore(Long id) {
        LOG.debug("Restoring deleted entity with id: {}", id);

        ENTITY entity = getEntityById(id);

        if (!entity.isDeleted()) {
            LOG.warn("Entity with id {} is not in deleted state", id);
            throw new IllegalStateException("Kayıt silinmemiş durumda, geri yüklenemez");
        }

        beforeRestore(entity);
        applyRestore(entity);
        ENTITY restoredEntity = repository.save(entity);
        afterRestore(restoredEntity);

        LOG.info("Entity restored successfully with id: {}", id);
        return mapper.toDto(restoredEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DTO> findAll(Specification<ENTITY> specification, Pageable pageable) {
        LOG.debug("Finding entities with specification and pagination: {}", pageable);
        Specification<ENTITY> spec = specification != null ? specification : Specification.where(null);
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DTO> findAll(Specification<ENTITY> specification) {
        LOG.debug("Finding all entities with specification");
        Specification<ENTITY> spec = specification != null ? specification : Specification.where(null);
        return repository.findAll(spec).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        LOG.debug("Checking if entity exists with id: {}", id);
        return id != null && repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<ENTITY> specification) {
        LOG.debug("Counting entities with specification");
        Specification<ENTITY> spec = specification != null ? specification : Specification.where(null);
        return repository.count(spec);
    }

    // Protected helper methods

    /**
     * Retrieves an entity by ID or throws EntityNotFoundException.
     *
     * @param id the entity ID
     * @return the entity
     * @throws EntityNotFoundException if entity not found
     */
    protected ENTITY getEntityById(Long id) {
        if (id == null) {
            LOG.error("Entity ID cannot be null");
            throw new IllegalArgumentException("ID boş olamaz");
        }

        return repository.findById(id)
                .orElseThrow(() -> {
                    LOG.error("Entity not found with id: {}", id);
                    return new EntityNotFoundException("Kayıt bulunamadı: " + id);
                });
    }

    /**
     * Validates create request. Override for custom validation.
     *
     * @param request the create request
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateCreateRequest(REQUEST request) {
        if (request == null) {
            LOG.error("Create request cannot be null");
            throw new IllegalArgumentException("Gönderilen veri boş olamaz");
        }
    }

    /**
     * Validates update request. Override for custom validation.
     *
     * @param request the update request
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateUpdateRequest(REQUEST request) {
        if (request == null) {
            LOG.error("Update request cannot be null");
            throw new IllegalArgumentException("Gönderilen veri boş olamaz");
        }
    }

    /**
     * Ensures entity is not in deleted state.
     *
     * @param entity the entity to check
     * @throws IllegalStateException if entity is deleted
     */
    protected void ensureNotDeleted(ENTITY entity) {
        if (entity.isDeleted()) {
            LOG.error("Cannot operate on deleted entity with id: {}", entity.getId());
            throw new IllegalStateException("Silinmiş kayıt üzerinde işlem yapılamaz");
        }
    }

    /**
     * Applies soft delete to entity, setting deleted flag and audit information.
     *
     * @param entity the entity to soft delete
     */
    protected void applySoftDelete(ENTITY entity) {
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());

        Authentication authentication = SecurityUtils.getAuthentication();
        if (SecurityUtils.isAuthenticated(authentication)) {
            Long currentUserId = SecurityUtils.getAuthorizedUserDetails().getId();
            entity.setDeletedBy(currentUserId);
            LOG.debug("Entity soft deleted by user: {}", currentUserId);
        } else {
            LOG.debug("Entity soft deleted without authenticated user");
        }
    }

    /**
     * Applies restore to entity, clearing deleted flag and audit information.
     * You may want to add restoredBy/restoredAt fields to track restoration.
     *
     * @param entity the entity to restore
     */
    protected void applyRestore(ENTITY entity) {
        entity.setDeleted(false);
        entity.setDeletedAt(null);
        entity.setDeletedBy(0L);

        Authentication authentication = SecurityUtils.getAuthentication();
        if (SecurityUtils.isAuthenticated(authentication)) {
            Long currentUserId = SecurityUtils.getAuthorizedUserDetails().getId();
            LOG.debug("Entity restored by user: {}", currentUserId);
            entity.setDeletedBy(currentUserId);
            // Consider adding: entity.setRestoredBy(currentUserId);
            // Consider adding: entity.setRestoredAt(LocalDateTime.now());
        } else {
            LOG.debug("Entity restored without authenticated user");
        }
    }

    // Abstract methods for subclasses to implement

    /**
     * Maps a request object to a new entity instance.
     * This method is called during create operations.
     *
     * @param request the request object
     * @return new entity instance
     */
    protected abstract ENTITY mapToEntity(REQUEST request);

    /**
     * Updates an existing entity with data from request.
     * This method is called during update operations.
     *
     * @param entity the entity to update
     * @param request the request object containing update data
     */
    protected abstract void updateEntity(ENTITY entity, REQUEST request);

    // Template method hooks for subclasses

    /**
     * Hook method called before entity creation.
     * Override to add custom logic before save.
     *
     * @param entity the entity about to be created
     */
    protected void beforeCreate(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called after entity creation.
     * Override to add custom logic after save (e.g., event publishing).
     *
     * @param entity the created entity
     */
    protected void afterCreate(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called before entity update.
     * Override to add custom logic before save.
     *
     * @param entity the entity about to be updated
     */
    protected void beforeUpdate(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called after entity update.
     * Override to add custom logic after save (e.g., cache invalidation).
     *
     * @param entity the updated entity
     */
    protected void afterUpdate(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called before entity soft deletion.
     * Override to add custom logic before delete.
     *
     * @param entity the entity about to be deleted
     */
    protected void beforeDelete(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called after entity soft deletion.
     * Override to add custom logic after delete (e.g., cleanup).
     *
     * @param entity the deleted entity
     */
    protected void afterDelete(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called before entity restoration.
     * Override to add custom logic before restore.
     *
     * @param entity the entity about to be restored
     */
    protected void beforeRestore(ENTITY entity) {
        // Template method - override in subclasses if needed
    }

    /**
     * Hook method called after entity restoration.
     * Override to add custom logic after restore.
     *
     * @param entity the restored entity
     */
    protected void afterRestore(ENTITY entity) {
        // Template method - override in subclasses if needed
    }
}
