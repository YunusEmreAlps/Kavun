package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.request.UserRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for User entity operations following AbstractService pattern.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper extends BaseMapper<UserRequest, User, UserDto> {

    @Override
    @Mapping(target = "role", expression = "java(com.kavun.shared.util.UserUtils.getTopmostRole(entity))")
    @Mapping(target = "profileImage", expression = "java(com.kavun.shared.util.UserUtils.getUserProfileImage(entity))")
    UserDto toDto(User entity);

    @Override
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "password", ignore = true) // Password should be handled separately for security
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "accountNonExpired", constant = "true")
    @Mapping(target = "accountNonLocked", constant = "true")
    @Mapping(target = "credentialsNonExpired", constant = "true")
    User toEntity(UserRequest request);
}
