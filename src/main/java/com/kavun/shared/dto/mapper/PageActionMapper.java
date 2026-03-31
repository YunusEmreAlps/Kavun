package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.repository.ActionRepository;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.shared.dto.PageActionDto;
import com.kavun.shared.request.PageActionRequest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper for PageAction entity and PageActionDto.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class PageActionMapper implements BaseMapper<PageActionRequest, PageAction, PageActionDto> {

    @Autowired
    protected PageRepository pageRepository;

    @Autowired
    protected ActionRepository actionRepository;

    @Override
    @Mapping(source = "page.id", target = "pageId")
    @Mapping(source = "action.id", target = "actionId")
    public abstract PageActionDto toDto(PageAction entity);

    @Override
    @Mapping(target = "page", expression = "java(mapWebPage(request.getPageId()))")
    @Mapping(target = "action", expression = "java(mapAction(request.getActionId()))")
    public abstract PageAction toEntity(PageActionRequest request);

    protected WebPage mapWebPage(Long pageId) {
        if (pageId == null) {
            throw new IllegalArgumentException("Page ID cannot be null");
        }
        return pageRepository.findById(pageId)
            .orElseThrow(() -> new IllegalArgumentException(
                "WebPage not found with ID: " + pageId));
    }

    protected Action mapAction(Long actionId) {
        if (actionId == null) {
            throw new IllegalArgumentException("Action ID cannot be null");
        }
        return actionRepository.findById(actionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Action not found with ID: " + actionId));
    }
}
