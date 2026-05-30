package com.taskflow.user;

import com.taskflow.common.exception.AppException;
import com.taskflow.user.dto.UserResponseDto;
import com.taskflow.user.dto.UserUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponseDto getUserProfile(Long userId) {
        User user = findUserById(userId);
        return userMapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateUserProfile(Long userId, UserUpdateDto dto) {
        User user = findUserById(userId);

        userMapper.updateEntityFromDto(dto, user);
        userRepository.save(user);
        return userMapper.toResponseDto(user);
    }

    // ── Internal helper used by other services ────────────────────
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }
}