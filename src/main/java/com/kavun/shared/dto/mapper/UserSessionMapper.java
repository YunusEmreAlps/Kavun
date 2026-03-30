package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.UserSession;
import com.kavun.shared.dto.UserSessionDto;
import com.kavun.shared.request.UserSessionRequest;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserSessionMapper extends BaseMapper<UserSessionRequest, UserSession, UserSessionDto> {
}
