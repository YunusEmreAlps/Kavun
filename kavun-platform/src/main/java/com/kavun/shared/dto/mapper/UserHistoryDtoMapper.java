package com.kavun.shared.dto.mapper;

import com.kavun.backend.persistent.domain.user.UserHistory;
import com.kavun.shared.dto.UserHistoryDto;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * The UserHistoryDtoMapper class outlines the supported conversions between UserHistory and other objects.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserHistoryDtoMapper {

  UserHistoryDtoMapper MAPPER = Mappers.getMapper(UserHistoryDtoMapper.class);

  /**
   * Convert and populate a userHistories to userHistoryDto object.
   *
   * @param userHistories the userHistories
   * @return the userHistoryDto
   */
  List<UserHistoryDto> toUserHistoryDto(Set<UserHistory> userHistories);

  /**
   * Convert and populate a userHistory to userHistoryDto object.
   *
   * @param userHistory the userHistory
   * @return the userHistoryDto
   */
  @Mapping(
      target = "timeElapsedDescription",
      expression =
          "java(com.kavun.shared.util.core.DateUtils.getTimeElapsedDescription(userHistory.getCreatedAt()))")
  @Mapping(
      target = "separateDateFormat",
      expression =
          "java(com.kavun.shared.util.core.DateUtils.getTimeElapsed(userHistory.getCreatedAt()))")
  UserHistoryDto toUserHistoryDto(UserHistory userHistory);
}
