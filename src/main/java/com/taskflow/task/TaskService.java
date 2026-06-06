package com.taskflow.task;

import com.taskflow.audit.AuditLogService;
import com.taskflow.common.JsonUtil;
import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import com.taskflow.common.enums.TaskPriority;
import com.taskflow.common.exception.AppException;
import com.taskflow.common.enums.TaskStatus;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectService;
import com.taskflow.task.dto.*;
import com.taskflow.task.event.TaskDeletedEvent;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    // ── Create Task ───────────────────────────────────────────────
    @Transactional
    public TaskResponseDto createTask(Long projectId, TaskRequestDto dto, Long userId) {
        Project project = projectService.findProjectById(projectId);
        User creator = userService.findUserById(userId);
        projectService.validateMember(project, creator);

        Task task = taskMapper.toEntity(dto);
        task.setProject(project);
        task.setCreatedBy(creator);

        if (dto.getAssigneeId() != null) {
            User assignee = userService.findUserById(dto.getAssigneeId());
            projectService.validateMember(project, assignee);
            task.setAssignee(assignee);
        }

        taskRepository.save(task);
        auditLogService.log(EntityType.TASK, task.getId(), AuditAction.CREATE, null, userId);
        return taskMapper.toResponseDto(task);
    }

    // ── Get All Tasks ─────────────────────────────────────────────
    @Transactional
    public Page<TaskResponseDto> getTasks(Long projectId, Long userId,
                                          TaskStatus status, String priority,
                                          Long assigneeId, String sortBy,
                                          String order, int page, int size) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (status != null) {
            return taskRepository
                    .findByProjectAndStatusAndParentIsNullAndIsDeletedFalse(project, status, pageable)
                    .map(taskMapper::toResponseDto);
        }

        if (assigneeId != null) {
            User assignee = userService.findUserById(assigneeId);
            return taskRepository
                    .findByProjectAndAssigneeAndParentIsNullAndIsDeletedFalse(project, assignee, pageable)
                    .map(taskMapper::toResponseDto);
        }

        if (priority != null) {
            TaskPriority taskPriority = TaskPriority.valueOf(priority.toUpperCase());
            return taskRepository
                    .findByProjectAndPriorityAndParentIsNullAndIsDeletedFalse(project, taskPriority, pageable)
                    .map(taskMapper::toResponseDto);
        }

        return taskRepository
                .findByProjectAndParentIsNullAndIsDeletedFalse(project, pageable)
                .map(taskMapper::toResponseDto);
    }

    // ── Get Task By Id ────────────────────────────────────────────
    @Transactional
    public TaskResponseDto getTaskById(Long projectId, Long taskId, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task task = findTaskById(taskId, projectId);
        return taskMapper.toResponseDto(task);
    }

    // ── Update Task ───────────────────────────────────────────────
    @Transactional
    public TaskResponseDto updateTask(Long projectId, Long taskId,
                                      TaskUpdateDto dto, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task task = findTaskById(taskId, projectId);

        Map<String, Object> changes = new HashMap<>();
        if (dto.getTitle() != null && !dto.getTitle().equals(task.getTitle()))
            changes.put("title", Map.of("old", task.getTitle(), "new", dto.getTitle()));
        if (dto.getPriority() != null && !dto.getPriority().equals(task.getPriority()))
            changes.put("priority", Map.of("old", task.getPriority(), "new", dto.getPriority()));
        if (dto.getDescription() != null && !dto.getDescription().equals(task.getDescription()))
            changes.put("description", Map.of("old", task.getDescription(), "new", dto.getDescription()));
        if (dto.getStartDate() != null && !dto.getStartDate().equals(task.getStartDate()))
            changes.put("startDate", Map.of("old", task.getStartDate(), "new", dto.getStartDate()));
        if (dto.getDueDate() != null && !dto.getDueDate().equals(task.getDueDate()))
            changes.put("dueDate", Map.of("old", task.getDueDate(), "new", dto.getDueDate()));
        if (dto.getAssigneeId() != null)
            changes.put("assigneeId", Map.of(
                    "old", task.getAssignee() != null ? task.getAssignee().getId() : "null",
                    "new", dto.getAssigneeId()));

        if (dto.getAssigneeId() != null) {
            User assignee = userService.findUserById(dto.getAssigneeId());
            projectService.validateMember(project, assignee);
            task.setAssignee(assignee);
        }

        taskMapper.updateEntityFromDto(dto, task);
        Task updatedTask = taskRepository.save(task);

        auditLogService.log(EntityType.TASK, taskId, AuditAction.UPDATE,
                JsonUtil.toJson(changes), userId);

        return taskMapper.toResponseDto(updatedTask);
    }

    // ── Update Task Status ────────────────────────────────────────
    @Transactional
    public TaskResponseDto updateTaskStatus(Long projectId, Long taskId,
                                            TaskStatusUpdateDto dto, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task task = findTaskById(taskId, projectId);
        validateStatusTransition(task.getStatus(), dto.getStatus());

        Map<String, Object> changes = new HashMap<>();
        changes.put("status", Map.of("old", task.getStatus(), "new", dto.getStatus()));

        task.setStatus(dto.getStatus());
        taskRepository.save(task);

        auditLogService.log(EntityType.TASK, taskId, AuditAction.UPDATE,
                JsonUtil.toJson(changes), userId);

        return taskMapper.toResponseDto(task);
    }

    // ── Delete Task ───────────────────────────────────────────────
    @Transactional
    public void deleteTask(Long projectId, Long taskId, Long userId) {
        Project project = projectService.findProjectById(projectId);

        projectService.validateOwner(project, userId);

        Task task = findTaskById(taskId, projectId);

        // soft delete subtasks first
        List<Task> subTasks = taskRepository.findByParentAndIsDeletedFalse(task);
        subTasks.forEach(subTask -> {
            subTask.softDelete();
            taskRepository.save(subTask);
            eventPublisher.publishEvent(new TaskDeletedEvent(subTask.getId(), userId));
            auditLogService.log(EntityType.TASK, subTask.getId(), AuditAction.DELETE, null, userId);
        });

        task.softDelete();
        taskRepository.save(task);

        auditLogService.log(EntityType.TASK, taskId, AuditAction.DELETE, null, userId);

        eventPublisher.publishEvent(new TaskDeletedEvent(taskId, userId));
    }

    // ── Create Subtask ────────────────────────────────────────────
    @Transactional
    public TaskResponseDto createSubTask(Long projectId, Long taskId,
                                         TaskRequestDto dto, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task parentTask = findTaskById(taskId, projectId);

        if (parentTask.getParent() != null) {
            throw new AppException("Subtasks cannot have subtasks", HttpStatus.BAD_REQUEST);
        }

        User creator = userService.findUserById(userId);
        Task subTask = taskMapper.toEntity(dto);
        subTask.setProject(project);
        subTask.setCreatedBy(creator);
        subTask.setParent(parentTask);

        if (dto.getAssigneeId() != null) {
            User assignee = userService.findUserById(dto.getAssigneeId());
            projectService.validateMember(project, assignee);
            subTask.setAssignee(assignee);
        }

        taskRepository.save(subTask);
        return taskMapper.toResponseDto(subTask);
    }

    // ── Get Subtasks ──────────────────────────────────────────────
    @Transactional
    public List<TaskResponseDto> getSubTasks(Long projectId, Long taskId, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task parentTask = findTaskById(taskId, projectId);
        return taskRepository.findByParentAndIsDeletedFalse(parentTask)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    // ── Delete Subtask ────────────────────────────────────────────
    @Transactional
    public void deleteSubTask(Long projectId, Long taskId, Long subtaskId, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateOwner(project, userId);
        findTaskById(taskId, projectId);
        Task subTask = findTaskById(subtaskId, projectId);
        subTask.softDelete();
        taskRepository.save(subTask);
        auditLogService.log(EntityType.TASK, subtaskId, AuditAction.DELETE, null, userId);
        eventPublisher.publishEvent(new TaskDeletedEvent(subtaskId, userId));
    }

    // ── Internal Helpers ──────────────────────────────────────────
    public Task findTaskById(Long taskId, Long projectId) {
        return taskRepository.findByIdAndProjectIdAndIsDeletedFalse(taskId, projectId)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
    }

    private void validateStatusTransition(TaskStatus current, TaskStatus next) {
        boolean valid = switch (current) {
            case TODO -> next == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TaskStatus.IN_REVIEW;
            case IN_REVIEW -> next == TaskStatus.DONE;
            case DONE -> false;
        };
        if (!valid) {
            throw new AppException(
                    "Invalid status transition: " + current + " → " + next,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    public void deleteAllTasksByProjectId(Long projectId, Long userId) {
        List<Task> tasks = taskRepository.findByProjectIdAndIsDeletedFalse(projectId);
        if (tasks.isEmpty()) return;

        tasks.forEach(task -> {
            task.softDelete();
            taskRepository.save(task);
            auditLogService.log(EntityType.TASK, task.getId(), AuditAction.CASCADE_DELETE, null, userId);
            eventPublisher.publishEvent(new TaskDeletedEvent(task.getId(), userId));
        });
    }
}