package com.taskflow.comment.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentResponseDto {

    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private Long taskId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}