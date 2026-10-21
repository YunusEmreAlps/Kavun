package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.shared.dto.ActionDto;
import com.kavun.shared.request.ActionRequest;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for Action entity and ActionDto.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActionMapper extends BaseMapper<ActionRequest, Action, ActionDto> {
}
