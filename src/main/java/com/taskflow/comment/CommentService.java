package com.taskflow.comment;

import com.taskflow.audit.AuditLogService;
import com.taskflow.common.JsonUtil;
import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import com.taskflow.common.exception.AppException;
import com.taskflow.comment.dto.CommentRequestDto;
import com.taskflow.comment.dto.CommentResponseDto;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectService;
import com.taskflow.task.Task;
import com.taskflow.task.TaskService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    // ── Add Comment ───────────────────────────────────────────────
    @Transactional
    public CommentResponseDto addComment(Long taskId, Long projectId,
                                         CommentRequestDto dto, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task task = taskService.findTaskById(taskId, projectId);
        User author = userService.findUserById(userId);

        Comment comment = commentMapper.toEntity(dto);
        comment.setTask(task);
        comment.setAuthor(author);
        commentRepository.save(comment);

        auditLogService.log(EntityType.COMMENT, comment.getId(), AuditAction.CREATE, null, userId);

        return commentMapper.toResponseDto(comment);
    }

    // ── Get Comments ──────────────────────────────────────────────
    @Transactional
    public Page<CommentResponseDto> getComments(Long taskId, Long projectId,
                                                Long userId, String sortBy,
                                                String order, int page, int size) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        Task task = taskService.findTaskById(taskId, projectId);

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return commentRepository.findByTaskAndIsDeletedFalse(task, pageable)
                .map(commentMapper::toResponseDto);
    }

    // ── Update Comment ────────────────────────────────────────────
    @Transactional
    public CommentResponseDto updateComment(Long taskId, Long projectId,
                                            Long commentId, CommentRequestDto dto,
                                            Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        taskService.findTaskById(taskId, projectId);

        Comment comment = findCommentById(commentId);
        validateAuthor(commentId, userId);

        Map<String, Object> changes = new HashMap<>();
        if (!dto.getContent().equals(comment.getContent()))
            changes.put("content", Map.of("old", comment.getContent(), "new", dto.getContent()));

        commentMapper.updateEntityFromDto(dto, comment);
        commentRepository.save(comment);

        auditLogService.log(EntityType.COMMENT, commentId, AuditAction.UPDATE,
                JsonUtil.toJson(changes), userId);

        return commentMapper.toResponseDto(comment);
    }

    // ── Delete Comment ────────────────────────────────────────────
    @Transactional
    public void deleteComment(Long taskId, Long projectId,
                              Long commentId, Long userId) {
        Project project = projectService.findProjectById(projectId);
        projectService.validateMember(project, userId);
        taskService.findTaskById(taskId, projectId);

        Comment comment = findCommentById(commentId);
        validateAuthor(commentId, userId);

        comment.softDelete();
        commentRepository.save(comment);

        auditLogService.log(EntityType.COMMENT, commentId, AuditAction.DELETE, null, userId);

    }

    // ── Internal Helpers ──────────────────────────────────────────
    private Comment findCommentById(Long commentId) {
        return commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));
    }

    private void validateAuthor(Long commentId, Long userId) {
        if (!commentRepository.existsByIdAndAuthor_IdAndIsDeletedFalse(commentId, userId)) {
            throw new AppException("Can only edit your own comment", HttpStatus.FORBIDDEN);
        }
    }

    public void deleteAllCommentsByTaskId(Long taskId, Long userId) {
       List<Comment> comments = commentRepository.findByTaskIdAndIsDeletedFalse(taskId);
       if (comments.isEmpty())
           return;

        comments.forEach(comment -> {
            comment.softDelete();
            commentRepository.save(comment);
            auditLogService.log(EntityType.COMMENT, comment.getId(), AuditAction.CASCADE_DELETE, null, userId);
        });
    }
}