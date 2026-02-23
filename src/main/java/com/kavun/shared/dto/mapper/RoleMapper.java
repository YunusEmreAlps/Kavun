package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.request.RoleRequest;
import com.kavun.web.payload.response.UserRoleResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

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

    @Mapping(target = "id", expression = "java(user.getId())")
    UserRoleResponse toUserRoleResponse(User user);

    List<UserRoleResponse> toUserRoleResponseList(List<User> users);
}
