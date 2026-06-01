package com.taskflow.comment;

import com.taskflow.comment.dto.CommentRequestDto;
import com.taskflow.comment.dto.CommentResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(commentService.addComment(taskId, projectId, dto, userId));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponseDto>> getComments(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(commentService.getComments(
                taskId, projectId, userId, sortBy, order, page, size));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(commentService.updateComment(
                taskId, projectId, commentId, dto, userId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        commentService.deleteComment(taskId, projectId, commentId, userId);
        return ResponseEntity.noContent().build();
    }
}