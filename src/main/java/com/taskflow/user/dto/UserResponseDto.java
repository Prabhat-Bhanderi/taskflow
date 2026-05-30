package com.taskflow.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}