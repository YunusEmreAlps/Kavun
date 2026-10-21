package com.example.notes;

import com.kavun.shared.dto.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NoteMapper extends BaseMapper<NoteRequest, Note, NoteDto> {}
