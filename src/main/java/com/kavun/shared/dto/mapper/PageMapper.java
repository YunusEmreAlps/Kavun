package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.shared.dto.PageDto;
import com.kavun.shared.request.PageRequest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for Page entity and PageDto.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PageMapper extends BaseMapper<PageRequest, WebPage, PageDto> {

    @Override
    @Mapping(source = "parent.id", target = "parentId")
    PageDto toDto(WebPage entity);
}
