package com.taskflow.project;

import com.taskflow.user.User;
import com.taskflow.common.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByUser_IdAndIsDeletedFalse(Long userId);

    List<ProjectMember> findByProject_IdAndIsDeletedFalse(Long projectId);

    boolean existsByProjectAndUserAndIsDeletedFalse(Project project, User user);

    boolean existsByProjectAndUserAndRole(Project project, User user, ProjectRole role);

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId , Long userId);
}
