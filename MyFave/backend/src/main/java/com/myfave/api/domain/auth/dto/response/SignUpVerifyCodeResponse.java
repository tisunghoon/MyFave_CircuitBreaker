package com.myfave.api.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignUpVerifyCodeResponse {

    private String verifiedToken;

    public static SignUpVerifyCodeResponse of(String verifiedToken) {
        return new SignUpVerifyCodeResponse(verifiedToken);
    }
}
