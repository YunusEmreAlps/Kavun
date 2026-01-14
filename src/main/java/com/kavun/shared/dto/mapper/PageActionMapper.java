package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.shared.dto.PageActionDto;
import com.kavun.shared.request.PageActionRequest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for PageAction entity and PageActionDto.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PageActionMapper extends BaseMapper<PageActionRequest, PageAction, PageActionDto> {

    @Override
    @Mapping(source = "page.id", target = "pageId")
    @Mapping(source = "action.id", target = "actionId")
    PageActionDto toDto(PageAction entity);
}
