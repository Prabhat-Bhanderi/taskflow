package com.taskflow.project.dto;

import com.taskflow.common.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateDto {

    @NotNull(message = "Role is required")
    private ProjectRole role;
}