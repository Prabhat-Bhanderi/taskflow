package com.taskflow.task;

import com.taskflow.project.Project;
import com.taskflow.user.User;
import com.taskflow.common.enums.TaskPriority;
import com.taskflow.common.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // root tasks only (no subtasks)
    Page<Task> findByProjectAndParentIsNullAndIsDeletedFalse(Project project, Pageable pageable);

    // filter by status
    Page<Task> findByProjectAndStatusAndParentIsNullAndIsDeletedFalse(Project project, TaskStatus status, Pageable pageable);

    // filter by priority
    Page<Task> findByProjectAndPriorityAndParentIsNullAndIsDeletedFalse(Project project, TaskPriority priority, Pageable pageable);

    // filter by assignee
    Page<Task> findByProjectAndAssigneeAndParentIsNullAndIsDeletedFalse(Project project, User assignee, Pageable pageable);

    // get single task
    Optional<Task> findByIdAndProjectIdAndIsDeletedFalse(Long id, Long projectId);

    // get all subtasks of a task
    List<Task> findByParentAndIsDeletedFalse(Task parent);

    List<Task> findByProjectIdAndIsDeletedFalse(Long projectId);
}