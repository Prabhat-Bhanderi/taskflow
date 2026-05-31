package com.taskflow.task.dto;

import com.taskflow.common.enums.TaskPriority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskUpdateDto {

    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Long assigneeId;
}