package com.myfave.api.domain.auth.dto.response;

import com.myfave.api.domain.user.entity.User;
import lombok.Getter;

@Getter
public class SignUpResponse {

    private final Long userId;
    private final String nickname;

    private SignUpResponse(Long userId, String nickname) { // SignUpResponse 정의
        this.userId = userId;
        this.nickname = nickname;
    }

    public static SignUpResponse from(User user) {// 회원 가입 완료되었을시 UserId,nickname 리턴
        return new SignUpResponse(user.getUserId(), user.getNickname());
    }
}
