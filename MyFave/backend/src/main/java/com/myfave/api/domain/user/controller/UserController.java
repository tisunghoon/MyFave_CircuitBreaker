package com.myfave.api.domain.user.controller;

import com.myfave.api.domain.user.dto.request.UserUpdateRequest;
import com.myfave.api.domain.user.dto.response.UserResponse;
import com.myfave.api.domain.user.service.UserService;
import com.myfave.api.global.common.ApiResponse;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 2-1. 회원 정보 조회 (본인만)
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId) {
        if (!userId.equals(currentUserId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 2-2. 회원 정보 수정 (본인만)
    @PatchMapping("/{userId}/edit")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId,
            @RequestBody @Valid UserUpdateRequest request) {
        if (!userId.equals(currentUserId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(
                new ApiResponse<>(200, "회원 정보가 수정되었습니다.", response));
    }
}