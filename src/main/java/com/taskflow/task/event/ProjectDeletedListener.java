package com.taskflow.task.event;

import com.taskflow.project.event.ProjectDeletedEvent;
import com.taskflow.task.TaskService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectDeletedListener {

    private final TaskService taskService;

    @EventListener
    @Transactional
    public void onProjectDeletedEvent(ProjectDeletedEvent projectDeletedEvent) {

        taskService.deleteAllTasksByProjectId(projectDeletedEvent.getProjectId(), projectDeletedEvent.getUserId());

    }
}
