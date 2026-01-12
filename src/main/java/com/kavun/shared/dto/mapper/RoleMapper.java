package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.request.RoleRequest;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for Role entity and RoleDto.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper extends BaseMapper<RoleRequest, Role, RoleDto> {

    @Override
    Role toEntity(RoleRequest request);
}
