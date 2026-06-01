package com.taskflow.comment;

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

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

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

        commentMapper.updateEntityFromDto(dto, comment);
        commentRepository.save(comment);
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
}