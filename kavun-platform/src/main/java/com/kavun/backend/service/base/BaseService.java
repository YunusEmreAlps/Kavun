package com.kavun.backend.service.base;

import com.kavun.shared.dto.BaseDto;
import com.kavun.shared.request.BaseRequest;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Base service interface defining common CRUD operations.
 *
 * <p>List queries take filters as plain parameters (see {@link
 * com.kavun.backend.persistent.specification.FilterSpecification}). Soft deleted records are left
 * out unless {@code deleted=true} is passed.
 *
 * @param <REQUEST> Request type for create and update operations
 * @param <DTO> Data Transfer Object type for responses
 */
public interface BaseService<REQUEST extends BaseRequest, DTO extends BaseDto> {

  /**
   * Find an entity by its ID.
   *
   * @param id the entity ID
   * @return the entity DTO
   * @throws jakarta.persistence.EntityNotFoundException if entity not found
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
   * @throws jakarta.persistence.EntityNotFoundException if entity not found
   * @throws IllegalArgumentException if request is null
   * @throws IllegalStateException if entity is deleted
   */
  DTO update(Long id, REQUEST request);

  /**
   * Soft delete an entity.
   *
   * @param id the entity ID
   * @throws jakarta.persistence.EntityNotFoundException if entity not found
   * @throws IllegalStateException if entity is already deleted
   */
  void delete(Long id);

  /**
   * Restore a soft-deleted entity.
   *
   * @param id the entity ID
   * @return the restored entity DTO
   * @throws jakarta.persistence.EntityNotFoundException if entity not found
   * @throws IllegalStateException if entity is not deleted
   */
  DTO restore(Long id);

  /**
   * Find the entities matching the filters, one page at a time.
   *
   * @param filters the filter parameters
   * @param pageable pagination information
   * @return page of entity DTOs
   */
  Page<DTO> findAll(Map<String, ?> filters, Pageable pageable);

  /**
   * Find all entities matching the filters.
   *
   * @param filters the filter parameters
   * @return list of entity DTOs
   */
  List<DTO> findAll(Map<String, ?> filters);

  /**
   * Check if an entity exists by ID.
   *
   * @param id the entity ID
   * @return true if entity exists, false otherwise
   */
  boolean existsById(Long id);

  /**
   * Count the entities matching the filters.
   *
   * @param filters the filter parameters
   * @return number of matching entities
   */
  long count(Map<String, ?> filters);
}
