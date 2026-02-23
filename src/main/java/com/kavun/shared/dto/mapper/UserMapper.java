package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.domain.user.UserRole;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.enums.OtpDeliveryMethod;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.request.UserRequest;
import com.kavun.web.payload.response.UserResponse;
import com.kavun.shared.util.UserUtils;

import java.util.HashSet;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for User entity operations following AbstractService pattern.
 * This mapper consolidates all User-related mappings previously split between UserMapper.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
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

    // Convert and populate a UserRequest to userDto object.
    UserDto toUserDto(UserRequest request);

    /**
     * Convert and populate a userDetailsBuilder to userDto object.
     *
     * @param userDetailsBuilder the userDetailsBuilder
     * @return the userDto
     */
    UserDto toUserDto(UserDetailsBuilder userDetailsBuilder);

    /**
     * Convert and populate a userDto to User object.
     *
     * @param userDto the userDto
     * @return the user
     */
    @Mapping(target = "otpDeliveryMethod", source = "otpDeliveryMethod", qualifiedByName = "mapOtpDeliveryMethod")
    User toUser(UserDto userDto);

    /**
     * Convert and populate a User to UserResponse object.
     *
     * @param user the user
     * @return the userResponse
     */
    @Mapping(target = "createdBy", expression = "java(mapToAuditInfo(user.getCreatedBy()))")
    @Mapping(target = "updatedBy", expression = "java(mapToAuditInfo(user.getUpdatedBy()))")
    @Mapping(target = "deletedBy", expression = "java(mapToAuditInfo(user.getDeletedBy()))")
    @Mapping(target = "roles", expression = "java(mapUserRolesToAuditInfo(user.getUserRoles()))")
    UserResponse toUserResponse(User user);

    /**
     * Maps UUID to AuditInfo object with id and name.
     *
     * @param userId the user UUID
     * @return AuditInfo object or null if userId is null
     */
    default UserResponse.AuditInfo mapToAuditInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        String fullName = UserUtils.getUserFullName(userId);
        return new UserResponse.AuditInfo(userId, fullName);
    }

        // Maps a set of UserRoles to a set of AuditInfo objects with id and name.
    default Set<UserResponse.AuditInfo> mapUserRolesToAuditInfo(Set<UserRole> userRoles) {
        Set<UserResponse.AuditInfo> auditInfoSet = new HashSet<>();
        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null) {
                auditInfoSet.add(new UserResponse.AuditInfo(userRole.getRole().getId(), userRole.getRole().getName()));
            }
        }
        return auditInfoSet;
    }

    /**
     * Maps OTP delivery method, providing default value if null.
     *
     * @param otpDeliveryMethod the OTP delivery method from DTO
     * @return the OTP delivery method or default EMAIL
     */
    @Named("mapOtpDeliveryMethod")
    default String mapOtpDeliveryMethod(String otpDeliveryMethod) {
        return otpDeliveryMethod != null ? otpDeliveryMethod : OtpDeliveryMethod.EMAIL.name();
    }
}
