package com.taskflow.task;

import com.taskflow.common.enums.TaskPriority;
import com.taskflow.common.exception.AppException;
import com.taskflow.common.enums.TaskStatus;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectMemberRepository;
import com.taskflow.project.ProjectService;
import com.taskflow.task.dto.*;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectService projectService;
    private final UserService userService;

    // ── Create Task ───────────────────────────────────────────────
    @Transactional
    public TaskResponseDto createTask(Long projectId, TaskRequestDto dto, Long userId) {
        Project project = projectService.findProjectById(projectId);
        User creator = userService.findUserById(userId);
        projectService.validateMember(project, userId);

        Task task = taskMapper.toEntity(dto);
        task.setProject(project);
        task.setCreatedBy(creator);

        if (dto.getAssigneeId() != null) {
            User assignee = userService.findUserById(dto.getAssigneeId());
            projectService.validateMember(project, assignee.getId());
            task.setAssignee(assignee);
        }

        taskRepository.save(task);
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

        if (dto.getAssigneeId() != null) {
            User assignee = userService.findUserById(dto.getAssigneeId());
            projectService.validateMember(project, assignee.getId());
            task.setAssignee(assignee);
        }

        taskMapper.updateEntityFromDto(dto, task);
        Task updatedTask = taskRepository.save(task);

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
        task.setStatus(dto.getStatus());
        taskRepository.save(task);
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
        });

        task.softDelete();
        taskRepository.save(task);
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
            projectService.validateMember(project, assignee.getId());
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
}