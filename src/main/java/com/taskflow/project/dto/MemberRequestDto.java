package com.taskflow.project.dto;

import com.taskflow.common.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    private ProjectRole role = ProjectRole.MEMBER;
}