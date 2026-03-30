package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.UserDevice;
import com.kavun.shared.dto.UserDeviceDto;
import com.kavun.shared.request.UserDeviceRequest;

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
public interface UserDeviceMapper extends BaseMapper<UserDeviceRequest, UserDevice, UserDeviceDto> {
}
