package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.tenant.Tenant;
import com.kavun.shared.dto.TenantDto;
import com.kavun.shared.request.TenantRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for tenants.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper extends BaseMapper<TenantRequest, Tenant, TenantDto> {}
