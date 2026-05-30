package com.taskflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequestDto {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
}