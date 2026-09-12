package com.myfave.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ResetPasswordRequest {

    @NotBlank
    private String passwordResetToken;

    @NotBlank
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String newPassword;

    // 해당 부분에 대한 검증은 AuthService 에서 실행함.
    @NotBlank
    private String newPasswordConfirm;
}
