package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.shared.dto.PermissionDto;
import com.kavun.shared.request.PermissionRequest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper for Permission entity and PermissionDto.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class PermissionMapper implements BaseMapper<PermissionRequest, Permission, PermissionDto> {

    @Autowired
    protected PageActionRepository pageActionRepository;

    @Override
    @Mapping(source = "pageAction.id", target = "pageActionId")
    public abstract PermissionDto toDto(Permission entity);

    @Override
    @Mapping(target = "pageAction", expression = "java(mapPageAction(request.getPageActionId()))")
    public abstract Permission toEntity(PermissionRequest request);

    protected PageAction mapPageAction(Long pageActionId) {
        if (pageActionId == null) {
            return null;
        }
        return pageActionRepository.findById(pageActionId).orElse(null);
    }
}
