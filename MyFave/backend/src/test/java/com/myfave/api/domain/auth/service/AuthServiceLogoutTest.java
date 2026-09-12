package com.myfave.api.domain.auth.service;

import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLogoutTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    private static final String ACCESS_TOKEN = "valid.access.token";
    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("정상 로그아웃 - Access Token 블랙리스트 등록 및 Refresh Token 삭제")
    void logout_success() {
        // given
        long remaining = 30_000L;
        given(jwtTokenProvider.validateToken(ACCESS_TOKEN)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(jwtTokenProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(remaining);
        given(jwtTokenProvider.getUserId(ACCESS_TOKEN)).willReturn(USER_ID);

        // when
        authService.logout(ACCESS_TOKEN);

        // then
        verify(valueOperations).set("blacklist:" + ACCESS_TOKEN, "logout", remaining, TimeUnit.MILLISECONDS);
        verify(redisTemplate).delete("refresh:" + USER_ID);
    }

    @Test
    @DisplayName("만료된 Access Token으로 로그아웃 - 블랙리스트 등록 생략, Refresh Token만 삭제")
    void logout_expiredAccessToken_skipsBlacklist() {
        // given
        given(jwtTokenProvider.validateToken(ACCESS_TOKEN)).willReturn(false);
        given(jwtTokenProvider.isExpiredToken(ACCESS_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(-1L); // 이미 만료
        given(jwtTokenProvider.getUserId(ACCESS_TOKEN)).willReturn(USER_ID);

        // when
        authService.logout(ACCESS_TOKEN);

        // then
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
        verify(redisTemplate).delete("refresh:" + USER_ID);
    }

    @Test
    @DisplayName("위변조된 Access Token으로 로그아웃 시도 - AUTH_UNAUTHORIZED 예외 발생")
    void logout_invalidAccessToken_throwsException() {
        // given
        String invalidToken = "invalid.tampered.token";
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);
        given(jwtTokenProvider.isExpiredToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.logout(invalidToken))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
                });

        verify(redisTemplate, never()).delete(anyString());
    }
}
