package com.taskflow.user;

import com.taskflow.user.dto.UserResponseDto;
import com.taskflow.user.dto.UserUpdateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateDto dto) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(userService.updateUserProfile(userId, dto));
    }
}