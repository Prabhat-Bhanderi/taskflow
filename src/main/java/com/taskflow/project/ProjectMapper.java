package com.taskflow.project;

import com.taskflow.project.dto.OwnerDto;
import com.taskflow.project.dto.ProjectRequestDto;
import com.taskflow.project.dto.ProjectResponseDto;
import com.taskflow.project.dto.ProjectUpdateDto;
import com.taskflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProjectMapper {

    @Mapping(target = "owner", ignore = true)
    Project toEntity(ProjectRequestDto dto);

    @Mapping(source = "owner", target = "owner")
    ProjectResponseDto toResponseDto(Project project);

    void updateEntityFromDto(ProjectUpdateDto dto, @MappingTarget Project project);

    OwnerDto toOwnerDto(User user);
}