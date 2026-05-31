package com.taskflow.task;

import com.taskflow.common.enums.TaskStatus;
import com.taskflow.task.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // ── Task CRUD ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskService.createTask(projectId, dto, userId));
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponseDto>> getTasks(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(taskService.getTasks(
                projectId, userId, status, priority,
                assigneeId, sortBy, order, page, size));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(taskService.getTaskById(projectId, taskId, userId));
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(taskService.updateTask(projectId, taskId, dto, userId));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponseDto> updateTaskStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(taskService.updateTaskStatus(projectId, taskId, dto, userId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.deleteTask(projectId, taskId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Subtask ───────────────────────────────────────────────────

    @PostMapping("/{taskId}/subtasks")
    public ResponseEntity<TaskResponseDto> createSubTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskService.createSubTask(projectId, taskId, dto, userId));
    }

    @GetMapping("/{taskId}/subtasks")
    public ResponseEntity<List<TaskResponseDto>> getSubTasks(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(taskService.getSubTasks(projectId, taskId, userId));
    }

    @PatchMapping("/{taskId}/subtasks/{subtaskId}")
    public ResponseEntity<TaskResponseDto> updateSubTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @Valid @RequestBody TaskUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(taskService.updateTask(projectId, subtaskId, dto, userId));
    }

    @DeleteMapping("/{taskId}/subtasks/{subtaskId}")
    public ResponseEntity<Void> deleteSubTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.deleteSubTask(projectId, taskId, subtaskId, userId);
        return ResponseEntity.noContent().build();
    }
}