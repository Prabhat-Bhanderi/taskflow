package com.taskflow.project;

import com.taskflow.project.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // Project CRUD operations

    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(
            @Valid @RequestBody ProjectRequestDto projectRequestDto,
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(projectService.createProject(projectRequestDto, userId));

    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> getUserProjects(
            @AuthenticationPrincipal UserDetails userDetails
            ){
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(projectService.getUserProjects(userId));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDto> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails
            ){
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(projectService.getProjectById(projectId, userId));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDto> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateDto projectUpdateDto,
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        return ResponseEntity.ok(projectService.updateProject(projectId, projectUpdateDto, userId));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.deleteProject(projectId, userId);
        return ResponseEntity.noContent().build();
    }


    // Project member management

    @PostMapping("/{projectId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MemberRequestDto memberRequestDto
            ){
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.addMember(projectId , memberRequestDto , userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberUpdateDto memberUpdateDto,
            @AuthenticationPrincipal UserDetails userDetails
            ){
        Long userId = Long.parseLong(userDetails.getUsername());

        projectService.updateMemberRole(projectId, memberId, memberUpdateDto, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetails userDetails
            ){
        Long userId = Long.parseLong(userDetails.getUsername());

        projectService.removeMember(projectId, memberId, userId);
        return ResponseEntity.noContent().build();
    }
}
