package com.taskflow.comment;

import com.taskflow.task.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByTaskAndIsDeletedFalse(Task task, Pageable pageable);

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    boolean existsByIdAndAuthor_IdAndIsDeletedFalse(Long id, Long authorId);
}
