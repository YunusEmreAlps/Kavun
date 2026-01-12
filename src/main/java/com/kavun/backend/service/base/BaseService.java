package com.kavun.backend.service.base;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.shared.dto.BaseDto;
import com.kavun.shared.request.BaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Base service interface defining common CRUD operations.
 * Uses separate request types for create and update operations.
 *
 * @param <CREATE_REQUEST> Request type for create operations
 * @param <UPDATE_REQUEST> Request type for update operations
 * @param <DTO> Data Transfer Object type for responses
 * @param <ENTITY> Entity type
 */
public interface BaseService<REQUEST extends BaseRequest, DTO extends BaseDto, ENTITY extends BaseEntity<Long>> {

  /**
   * Find an entity by its ID.
   *
   * @param id the entity ID
   * @return the entity DTO
   * @throws EntityNotFoundException if entity not found
   */
  DTO findById(Long id);

  /**
   * Create a new entity.
   *
   * @param request the creation request
   * @return the created entity DTO
   * @throws IllegalArgumentException if request is null
   */
  DTO create(REQUEST request);

  /**
   * Update an existing entity.
   *
   * @param id the entity ID
   * @param request the update request
   * @return the updated entity DTO
   * @throws EntityNotFoundException if entity not found
   * @throws IllegalArgumentException if request is null
   * @throws IllegalStateException if entity is deleted
   */
  DTO update(Long id,  REQUEST request);

  /**
   * Soft delete an entity.
   *
   * @param id the entity ID
   * @throws EntityNotFoundException if entity not found
   * @throws IllegalStateException if entity is already deleted
   */
  void delete(Long id);

  /**
   * Restore a soft-deleted entity.
   *
   * @param id the entity ID
   * @return the restored entity DTO
   * @throws EntityNotFoundException if entity not found
   * @throws IllegalStateException if entity is not deleted
   */
  DTO restore(Long id);

  /**
   * Find all entities matching the specification with pagination.
   *
   * @param specification the search specification
   * @param pageable pagination information
   * @return page of entity DTOs
   */
  Page<DTO> findAll(Specification<ENTITY> specification, Pageable pageable);

  /**
   * Find all entities matching the specification without pagination.
   *
   * @param specification the search specification
   * @return list of entity DTOs
   */
  List<DTO> findAll(Specification<ENTITY> specification);

  /**
   * Check if an entity exists by ID.
   *
   * @param id the entity ID
   * @return true if entity exists, false otherwise
   */
  boolean existsById(Long id);

  /**
   * Count entities matching the specification.
   *
   * @param specification the search specification
   * @return number of matching entities
   */
  long count(Specification<ENTITY> specification);
}
