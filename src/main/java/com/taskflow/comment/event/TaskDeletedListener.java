package com.taskflow.comment.event;

import com.taskflow.comment.CommentService;
import com.taskflow.task.event.TaskDeletedEvent;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskDeletedListener {

    private final CommentService commentService;

    @EventListener
    @Transactional
    public void onTaskDeletedEvent(TaskDeletedEvent taskDeletedEvent) {

        commentService.deleteAllCommentsByTaskId(taskDeletedEvent.getTaskId(), taskDeletedEvent.getUserId());
    }
}
