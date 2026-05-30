package com.taskflow.user;

import com.taskflow.user.dto.UserRequestDto;
import com.taskflow.user.dto.UserResponseDto;
import com.taskflow.user.dto.UserUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    User toEntity(UserRequestDto dto);

    UserResponseDto toResponseDto(User user);

    void updateEntityFromDto(UserUpdateDto dto, @MappingTarget User user);
}