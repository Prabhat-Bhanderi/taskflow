package com.taskflow.comment;

import com.taskflow.comment.dto.CommentRequestDto;
import com.taskflow.comment.dto.CommentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CommentMapper {

    @Mapping(target = "task", ignore = true)
    @Mapping(target = "author", ignore = true)
    Comment toEntity(CommentRequestDto dto);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.name", target = "authorName")
    @Mapping(source = "task.id", target = "taskId")
    CommentResponseDto toResponseDto(Comment comment);

    @Mapping(target = "task", ignore = true)
    @Mapping(target = "author", ignore = true)
    void updateEntityFromDto(CommentRequestDto dto, @MappingTarget Comment comment);
}