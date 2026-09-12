package com.myfave.api.domain.auth.dto.response;

import com.myfave.api.domain.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final Long userId;
    private final String nickname;

    private LoginResponse(String accessToken, String refreshToken, Long userId, String nickname) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.nickname = nickname;
    }

    public static LoginResponse of(String accessToken, String refreshToken, User user) {
        return new LoginResponse(accessToken, refreshToken, user.getUserId(), user.getNickname());
    }
}
