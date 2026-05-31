package com.taskflow.task;

import com.taskflow.task.dto.TaskRequestDto;
import com.taskflow.task.dto.TaskResponseDto;
import com.taskflow.task.dto.TaskUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "parent", ignore = true)
    Task toEntity(TaskRequestDto dto);

    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(source = "assignee.name", target = "assigneeName")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "createdBy.id", target = "createdBy")
    @Mapping(source = "subTasks", target = "subTasks")
    TaskResponseDto toResponseDto(Task task);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "parent", ignore = true)
    void updateEntityFromDto(TaskUpdateDto dto, @MappingTarget Task task);
}