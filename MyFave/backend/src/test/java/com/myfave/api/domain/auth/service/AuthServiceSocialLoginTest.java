package com.myfave.api.domain.auth.service;

import com.myfave.api.domain.auth.client.KakaoAuthClient;
import com.myfave.api.domain.auth.client.dto.KakaoTokenResponse;
import com.myfave.api.domain.auth.client.dto.KakaoUserInfoResponse;
import com.myfave.api.domain.auth.dto.request.SocialLoginRequest;
import com.myfave.api.domain.auth.dto.response.SocialLoginResponse;
import com.myfave.api.domain.user.entity.SocialProvider;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceSocialLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SocialLoginRequest request;
    private KakaoTokenResponse kakaoToken;
    private KakaoUserInfoResponse kakaoUserInfo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);

        request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "authorizationCode", "test-auth-code");

        kakaoToken = new KakaoTokenResponse();
        ReflectionTestUtils.setField(kakaoToken, "accessToken", "kakao-access-token");

        KakaoUserInfoResponse.KakaoAccount.Profile profile = new KakaoUserInfoResponse.KakaoAccount.Profile();
        ReflectionTestUtils.setField(profile, "nickname", "패션왕");
        KakaoUserInfoResponse.KakaoAccount account = new KakaoUserInfoResponse.KakaoAccount();
        ReflectionTestUtils.setField(account, "email", "user@kakao.com");
        ReflectionTestUtils.setField(account, "profile", profile);
        kakaoUserInfo = new KakaoUserInfoResponse();
        ReflectionTestUtils.setField(kakaoUserInfo, "id", 999L);
        ReflectionTestUtils.setField(kakaoUserInfo, "kakaoAccount", account);
    }

    @Test
    @DisplayName("신규 카카오 소셜 로그인 - 새 계정 생성, isNewUser=true 반환")
    void socialLogin_newUser_createsAccountAndReturnsIsNewUserTrue() {
        given(kakaoAuthClient.exchangeCodeForToken("test-auth-code")).willReturn(kakaoToken);
        given(kakaoAuthClient.getUserInfo("kakao-access-token")).willReturn(kakaoUserInfo);
        given(userRepository.findBySocialProviderId("999")).willReturn(Optional.empty());
        given(userRepository.findByEmail("user@kakao.com")).willReturn(Optional.empty());
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-password");

        User savedUser = User.builder()
                .email("user@kakao.com").password("encoded-password")
                .name("패션왕").nickname("패션왕").phone("SOCIAL_999")
                .socialProvider(SocialProvider.KAKAO).socialProviderId("999")
                .build();
        ReflectionTestUtils.setField(savedUser, "userId", 10L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.createAccessToken(10L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(10L)).willReturn("refresh-token");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        SocialLoginResponse response = authService.socialLogin("kakao", request);

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getNickname()).isEqualTo("패션왕");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("기존 카카오 소셜 로그인 - 기존 계정 반환, isNewUser=false")
    void socialLogin_existingUser_returnsIsNewUserFalse() {
        User existingUser = User.builder()
                .email("user@kakao.com").password("encoded-password")
                .name("패션왕").nickname("패션왕").phone("SOCIAL_999")
                .socialProvider(SocialProvider.KAKAO).socialProviderId("999")
                .build();
        ReflectionTestUtils.setField(existingUser, "userId", 10L);

        given(kakaoAuthClient.exchangeCodeForToken("test-auth-code")).willReturn(kakaoToken);
        given(kakaoAuthClient.getUserInfo("kakao-access-token")).willReturn(kakaoUserInfo);
        given(userRepository.findBySocialProviderId("999")).willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.createAccessToken(10L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(10L)).willReturn("refresh-token");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        SocialLoginResponse response = authService.socialLogin("kakao", request);

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getNickname()).isEqualTo("패션왕");
    }
}
