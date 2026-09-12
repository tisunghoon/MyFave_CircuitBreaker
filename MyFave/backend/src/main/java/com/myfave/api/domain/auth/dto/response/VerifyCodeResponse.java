package com.myfave.api.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor//● 모든 필드를 매개변수로 받는 생성자를 자동으로 만들어주는 Lombok 어노테이션
public class VerifyCodeResponse {

    private String passwordResetToken;

    public static VerifyCodeResponse of(String token) {
        return new VerifyCodeResponse(token);
    }
}
