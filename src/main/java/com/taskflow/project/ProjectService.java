package com.taskflow.project;

import com.taskflow.audit.AuditLogService;
import com.taskflow.common.JsonUtil;
import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import com.taskflow.common.enums.ProjectRole;
import com.taskflow.common.exception.AppException;
import com.taskflow.project.dto.*;
import com.taskflow.project.event.ProjectDeletedEvent;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProjectResponseDto createProject(@Valid ProjectRequestDto projectRequestDto, Long userId) {
        User user = userService.findUserById(userId);

        Project project = projectMapper.toEntity(projectRequestDto);
        project.setOwner(user);
        projectRepository.save(project);

        ProjectMember owner = new ProjectMember(project, user, ProjectRole.OWNER);
        projectMemberRepository.save(owner);

        auditLogService.log(EntityType.PROJECT, project.getId(), AuditAction.CREATE, null, userId);

        return projectMapper.toResponseDto(project);
    }

    @Transactional
    public List<ProjectResponseDto> getUserProjects(Long userId) {

        List<ProjectMember> members = projectMemberRepository.findByUser_IdAndIsDeletedFalse(userId);

        return members.stream()
                .map(member -> projectMapper.toResponseDto(member.getProject()))
                .toList();
    }

    @Transactional
    public ProjectResponseDto getProjectById(Long projectId, Long userId) {
        Project project = findProjectById(projectId);
        validateMember(project, userId);

        return projectMapper.toResponseDto(project);
    }

    @Transactional
    public ProjectResponseDto updateProject(Long projectId, @Valid ProjectUpdateDto projectUpdateDto, Long userId) {
        Project project = findProjectById(projectId);
        validateOwner(project, userId);

        Map<String, Object> changes = new HashMap<>();
        if (projectUpdateDto.getName() != null && !projectUpdateDto.getName().equals(project.getName()))
            changes.put("name", Map.of("old", project.getName(), "new", projectUpdateDto.getName()));
        if (projectUpdateDto.getDescription() != null && !projectUpdateDto.getDescription().equals(project.getDescription()))
            changes.put("description", Map.of("old", project.getDescription(), "new", projectUpdateDto.getDescription()));


        projectMapper.updateEntityFromDto(projectUpdateDto, project);
        projectRepository.save(project);

        auditLogService.log(EntityType.PROJECT, projectId, AuditAction.UPDATE,
                JsonUtil.toJson(changes), userId);

        return projectMapper.toResponseDto(project);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = findProjectById(projectId);
        validateOwner(project, userId);

        project.softDelete();
        projectRepository.save(project);

        projectMemberRepository.findByProject_IdAndIsDeletedFalse(projectId)
                .forEach(member -> {
                    member.softDelete();
                    projectMemberRepository.save(member);
                    auditLogService.log(EntityType.PROJECT_MEMBER, projectId, AuditAction.CASCADE_DELETE, null, userId);
                });

        auditLogService.log(EntityType.PROJECT, projectId, AuditAction.DELETE, null, userId);

        eventPublisher.publishEvent(new ProjectDeletedEvent(projectId, userId));
    }

    @Transactional
    public void addMember(Long projectId, @Valid MemberRequestDto memberRequestDto, Long userId) {
        Project project = findProjectById(projectId);
        validateOwner(project, userId);

        User newMember = userService.findUserById(memberRequestDto.getUserId());

        if(projectMemberRepository.existsByProjectAndUserAndIsDeletedFalse(project, newMember)) {
            throw new AppException("User is already a member of the project", HttpStatus.CONFLICT);
        }

        ProjectMember member = new ProjectMember(project, newMember, memberRequestDto.getRole());
        projectMemberRepository.save(member);

        auditLogService.log(EntityType.PROJECT_MEMBER, projectId, AuditAction.CREATE, null, userId);
    }

    @Transactional
    public void updateMemberRole(Long projectId, Long memberId, @Valid MemberUpdateDto memberUpdateDto, Long userId) {
        Project project = findProjectById(projectId);
        validateOwner(project, userId);

        ProjectMember isMember = projectMemberRepository.findByProject_IdAndUser_Id(projectId, memberId)
                .orElseThrow(() -> new AppException("Project member not found with id: " + memberId, HttpStatus.NOT_FOUND));
        
        isMember.setRole(memberUpdateDto.getRole());
        projectMemberRepository.save(isMember);
    }

    @Transactional
    public void removeMember(Long projectId, Long memberId, Long userId) {
        Project project = findProjectById(projectId);
        validateOwner(project, userId);

        ProjectMember isMember = projectMemberRepository.findByProject_IdAndUser_Id(projectId, memberId)
                .orElseThrow(() -> new AppException("Project member not found with id: " + memberId, HttpStatus.NOT_FOUND));

        if(isMember.getRole().equals(ProjectRole.OWNER)) {
            throw new AppException("Cannot remove the project owner", HttpStatus.CONFLICT);
        }

        isMember.softDelete();
        projectMemberRepository.save(isMember);

        auditLogService.log(EntityType.PROJECT_MEMBER, projectId, AuditAction.DELETE, null, userId);
    }

    // Internal Helper
    public Project findProjectById(Long projectId) {
        return projectRepository.findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new AppException("Project not found with id: " + projectId , HttpStatus.NOT_FOUND));
    }

    public void validateMember(Project project, Long userId) {
        User user = userService.findUserById(userId);

        if (!projectMemberRepository.existsByProjectAndUserAndIsDeletedFalse(project, user)) {
            throw new AppException( user.getName()+ " is not a project member", HttpStatus.FORBIDDEN);
        }
    }

    public void validateOwner(Project project, Long userId) {
        User user = userService.findUserById(userId);

        if (!projectMemberRepository.existsByProjectAndUserAndRole(project, user, ProjectRole.OWNER)) {
            throw new AppException("Only project owner can perform this action", HttpStatus.FORBIDDEN);
        }
    }
}
