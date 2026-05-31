package com.taskflow.task.dto;

import com.taskflow.common.enums.TaskPriority;
import com.taskflow.common.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TaskResponseDto {

    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Long assigneeId;
    private String assigneeName;
    private Long projectId;
    private Long createdBy;
    private List<TaskResponseDto> subTasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}