package com.taskflow.task.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskDeletedEvent {

    private final Long taskId;
    private final Long userId;

}
