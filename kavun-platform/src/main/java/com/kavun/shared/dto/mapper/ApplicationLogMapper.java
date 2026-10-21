package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import com.kavun.shared.dto.ApplicationLogDto;
import com.kavun.shared.request.ApplicationLogRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationLogMapper extends BaseMapper<ApplicationLogRequest, ApplicationLog, ApplicationLogDto> {

    @Override
    ApplicationLog toEntity(ApplicationLogRequest request);

    @Override
    ApplicationLogDto toDto(ApplicationLog entity);
}
