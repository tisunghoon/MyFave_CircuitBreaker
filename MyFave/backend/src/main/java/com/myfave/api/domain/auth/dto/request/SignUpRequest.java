package com.myfave.api.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
// 회원가입에 필요한 정보 정의
// 여기서 오류가 발생할 경우 ex) 이메일이 너무 짧음 Spring의 @Valid 어노테이션이 에러 발생
// Spring Boot 기본 설정(FAIL_ON_UNKNOWN_PROPERTIES=false)에 의존하지 않고 명시적으로 unknown 필드 무시.
// 프론트가 추후 profileImageUrl 같은 신규 필드를 보내도 400 으로 거절되지 않도록 보장 (PR#185 CR M4).
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class SignUpRequest {

    @NotBlank
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_]).{8,20}$",
            message = "비밀번호는 영문·숫자·특수문자를 포함한 8~20자여야 합니다."
    )
    private String password;

    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
    private String name;

    @NotBlank
    @Size(min = 2, max = 12)
    private String nickname;

    @NotBlank
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (010-XXXX-XXXX)")
    private String phone;

    @NotBlank
    private String verifiedToken;
}
