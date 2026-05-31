package com.taskflow.task.dto;

import com.taskflow.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateDto {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}