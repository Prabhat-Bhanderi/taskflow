package com.taskflow.user;

import com.taskflow.common.exception.AppException;
import com.taskflow.security.JwtUtil;
import com.taskflow.user.dto.AuthResponseDto;
import com.taskflow.user.dto.LoginRequestDto;
import com.taskflow.user.dto.RegisterRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerRequestDto) {
        if (userRepository.existsByEmail(registerRequestDto.getEmail())) {
            throw new AppException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(registerRequestDto.getName());
        user.setEmail(registerRequestDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponseDto(accessToken, refreshToken);
    }

    public AuthResponseDto login(LoginRequestDto loginRequestDto) {

        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponseDto(accessToken, refreshToken);
    }

    public AuthResponseDto refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new AppException("Refresh token expired or invalid", HttpStatus.UNAUTHORIZED);
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        return new AuthResponseDto(newAccessToken, newRefreshToken);
    }
}