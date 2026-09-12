package com.myfave.api.domain.auth.controller;

import com.myfave.api.domain.auth.dto.request.FindEmailRequest;
import com.myfave.api.domain.auth.dto.request.FindIdRequest;
import com.myfave.api.domain.auth.dto.request.LoginRequest;
import com.myfave.api.domain.auth.dto.request.PasswordResetSendCodeRequest;
import com.myfave.api.domain.auth.dto.request.ReissueRequest;
import com.myfave.api.domain.auth.dto.request.SignUpRequest;
import com.myfave.api.domain.auth.dto.request.SignUpSendCodeRequest;
import com.myfave.api.domain.auth.dto.request.SignUpVerifyCodeRequest;
import com.myfave.api.domain.auth.dto.request.ResetPasswordRequest;
import com.myfave.api.domain.auth.dto.request.SocialLoginRequest;
import com.myfave.api.domain.auth.dto.request.TempPasswordRequest;
import com.myfave.api.domain.auth.dto.request.VerifyCodeRequest;
import com.myfave.api.domain.auth.dto.response.FindEmailResponse;
import com.myfave.api.domain.auth.dto.response.FindIdResponse;
import com.myfave.api.domain.auth.dto.response.LoginResponse;
import com.myfave.api.domain.auth.dto.response.ReissueResponse;
import com.myfave.api.domain.auth.dto.response.SignUpResponse;
import com.myfave.api.domain.auth.dto.response.SignUpVerifyCodeResponse;
import com.myfave.api.domain.auth.dto.response.SocialLoginResponse;
import com.myfave.api.domain.auth.dto.response.VerifyCodeResponse;
import com.myfave.api.domain.auth.service.AuthService;
import com.myfave.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/send-code")
    public ApiResponse<Void> sendSignUpCode(@Valid @RequestBody SignUpSendCodeRequest request) {
        authService.sendSignUpCode(request);
        return new ApiResponse<>(200, "인증코드가 이메일로 발송되었습니다.", null);
    }

    @PostMapping("/signup/verify-code")
    public ApiResponse<SignUpVerifyCodeResponse> verifySignUpCode(@Valid @RequestBody SignUpVerifyCodeRequest request) {
        return new ApiResponse<>(200, "인증코드 확인 완료", authService.verifySignUpCode(request));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.created("회원가입이 완료되었습니다.", authService.signUp(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse<>(200, "로그인 성공", authService.login(request));
    }

    @PostMapping("/reissue")
    public ApiResponse<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return new ApiResponse<>(200, "토큰 재발급 성공", authService.reissue(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String bearerToken) {
        String accessToken = bearerToken.substring(7); // "Bearer " 제거
        authService.logout(accessToken);
        return new ApiResponse<>(200, "로그아웃 되었습니다.", null);
    }

    @PostMapping("/find-email")
    public ApiResponse<FindEmailResponse> findEmail(@Valid @RequestBody FindEmailRequest request) {
        return new ApiResponse<>(200, "이메일 조회 성공", authService.findEmail(request));
    }

    @PostMapping("/find-id")
    public ApiResponse<FindIdResponse> findId(@Valid @RequestBody FindIdRequest request) {
        return new ApiResponse<>(200, "OK", authService.findIdByNameAndPhone(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPasswordByEmail(@Valid @RequestBody TempPasswordRequest request) {
        authService.sendTempPassword(request);
        return new ApiResponse<>(200, "OK", null);
    }

    @PostMapping("/password-reset/send-code")
    public ApiResponse<Void> sendPasswordResetCode(@Valid @RequestBody PasswordResetSendCodeRequest request) {
        authService.sendPasswordResetCode(request);
        return new ApiResponse<>(200, "인증코드가 이메일로 발송되었습니다.", null);
    }

    @PostMapping("/password-reset/verify-code")
    public ApiResponse<VerifyCodeResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return new ApiResponse<>(200, "인증코드 확인 완료", authService.verifyCode(request));
    }

    @PutMapping("/password-reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new ApiResponse<>(200, "비밀번호가 재설정되었습니다.", null);
    }

    @PostMapping("/social-login/{provider}")
    public ApiResponse<SocialLoginResponse> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request) {
        return new ApiResponse<>(200, "소셜 로그인 성공", authService.socialLogin(provider, request));
    }
}
