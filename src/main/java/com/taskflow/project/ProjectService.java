package com.taskflow.project;

import com.taskflow.common.enums.ProjectRole;
import com.taskflow.common.exception.AppException;
import com.taskflow.project.dto.*;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final UserService userService;

    @Transactional
    public ProjectResponseDto createProject(@Valid ProjectRequestDto projectRequestDto, Long userId) {
        User user = userService.findUserById(userId);

        Project project = projectMapper.toEntity(projectRequestDto);
        project.setOwner(user);
        projectRepository.save(project);

        ProjectMember owner = new ProjectMember(project, user, ProjectRole.OWNER);
        projectMemberRepository.save(owner);

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

        projectMapper.updateEntityFromDto(projectUpdateDto, project);
        projectRepository.save(project);
        return projectMapper.toResponseDto(project);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = findProjectById(projectId);
        validateOwner(project, userId);

        project.softDelete();
        projectRepository.save(project);
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
