package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import com.kavun.shared.dto.BaseDto;
import com.kavun.shared.request.BaseRequest;

import java.util.List;
import java.util.Set;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Base mapper interface providing common mapping operations for entities and
 * DTOs.
 *
 * <p>
 * This interface should be extended by specific mappers to inherit common
 * mappings for audit
 * fields (createdAt, createdBy, updatedAt, updatedBy, version, publicId).
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
 * public interface UserMapper extends BaseMapper<User, UserDto> {
 *   // Additional custom mappings
 * }
 * }</pre>
 *
 * @param <E> the entity type extending BaseEntity
 * @param <D> the DTO type extending BaseDto
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public interface BaseMapper<BR extends BaseRequest, E extends BaseEntity, D extends BaseDto> {

  /**
   * Converts an entity to a DTO.
   *
   * @param entity the source entity
   * @return the mapped DTO, or null if entity is null
   */
  D toDto(E entity);

  /**
   * Converts a DTO to an entity.
   *
   * <p>
   * Note: Audit fields (createdAt, createdBy) are ignored as they should be
   * managed by JPA
   * auditing.
   *
   * @param request the source DTO
   * @return the mapped entity, or null if dto is null
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "deletedBy", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  E toEntity(BR request);

  /**
   * Converts a list of entities to a list of DTOs.
   *
   * @param entities the source entities
   * @return the list of mapped DTOs
   */
  List<D> toDtoList(List<E> entities);

  /**
   * Converts a set of entities to a list of DTOs.
   *
   * @param entities the source entities
   * @return the list of mapped DTOs
   */
  List<D> toDtoList(Set<E> entities);

  /**
   * Converts a list of DTOs to a list of entities.
   *
   * @param dtos the source DTOs
   * @return the list of mapped entities
   */
  List<E> toEntityList(List<D> dtos);

  /**
   * Updates an existing entity with values from a DTO.
   *
   * <p>
   * Null values in the DTO will not overwrite existing entity values. Audit
   * fields and ID are
   * preserved.
   *
   * @param request the source DTO with updated values
   * @param entity  the target entity to update
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "publicId", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "deletedBy", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  void updateEntityFromDto(BR request, @MappingTarget E entity);
}
