package com.myfave.api.domain.auth.service;

import com.myfave.api.domain.auth.client.KakaoAuthClient;
import com.myfave.api.domain.auth.dto.request.SignUpSendCodeRequest;
import com.myfave.api.domain.auth.dto.request.SignUpVerifyCodeRequest;
import com.myfave.api.domain.auth.dto.response.SignUpVerifyCodeResponse;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.global.mail.MailService;
import com.myfave.api.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignUpEmailTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private MailService mailService;
    @Mock private KakaoAuthClient kakaoAuthClient;

    @InjectMocks
    private AuthService authService;

    /* ────────────────── sendSignUpCode ────────────────── */

    @Test
    @DisplayName("이미 가입된 이메일로 인증코드 발송 시 USER_DUPLICATE_EMAIL 예외 발생")
    void sendSignUpCode_duplicateEmail_throwsException() {
        SignUpSendCodeRequest request = new SignUpSendCodeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        given(userRepository.existsByEmail("test@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.sendSignUpCode(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_DUPLICATE_EMAIL));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("5회 초과 발송 요청 시 AUTH_TOO_MANY_REQUESTS 예외 발생")
    void sendSignUpCode_exceedLimit_throwsException() {
        SignUpSendCodeRequest request = new SignUpSendCodeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("signup-code-count:test@example.com")).willReturn(6L);

        assertThatThrownBy(() -> authService.sendSignUpCode(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_TOO_MANY_REQUESTS));

        verify(mailService, never()).sendSignUpCode(anyString(), anyString());
    }

    @Test
    @DisplayName("첫 번째 발송 - Redis count TTL 설정 및 메일 발송")
    void sendSignUpCode_firstSend_success() {
        SignUpSendCodeRequest request = new SignUpSendCodeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("signup-code-count:test@example.com")).willReturn(1L);

        authService.sendSignUpCode(request);

        verify(redisTemplate).expire("signup-code-count:test@example.com", 5L, TimeUnit.MINUTES);
        verify(valueOperations).set(
                eq("signup-code:test@example.com"),
                anyString(),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        verify(mailService).sendSignUpCode(eq("test@example.com"), anyString());
    }

    /* ────────────────── verifySignUpCode ────────────────── */

    @Test
    @DisplayName("만료된 인증코드 검증 시 AUTH_EXPIRED_VERIFICATION_CODE 예외 발생")
    void verifySignUpCode_expired_throwsException() {
        SignUpVerifyCodeRequest request = new SignUpVerifyCodeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "verificationCode", "123456");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("signup-code:test@example.com")).willReturn(null);

        assertThatThrownBy(() -> authService.verifySignUpCode(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_EXPIRED_VERIFICATION_CODE));
    }

    @Test
    @DisplayName("잘못된 인증코드 입력 시 AUTH_INVALID_VERIFICATION_CODE 예외 발생")
    void verifySignUpCode_wrongCode_throwsException() {
        SignUpVerifyCodeRequest request = new SignUpVerifyCodeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "verificationCode", "999999");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("signup-code:test@example.com")).willReturn("123456");

        assertThatThrownBy(() -> authService.verifySignUpCode(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_INVALID_VERIFICATION_CODE));
    }

    @Test
    @DisplayName("정상 인증코드 검증 - 코드 삭제 후 verifiedToken 발급")
    void verifySignUpCode_success() {
        SignUpVerifyCodeRequest request = new SignUpVerifyCodeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "verificationCode", "123456");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("signup-code:test@example.com")).willReturn("123456");

        SignUpVerifyCodeResponse response = authService.verifySignUpCode(request);

        verify(redisTemplate).delete("signup-code:test@example.com");
        verify(valueOperations).set(
                startsWith("signup-verified-token:"),
                eq("test@example.com"),
                eq(10L),
                eq(TimeUnit.MINUTES)
        );
        assertThat(response.getVerifiedToken()).isNotBlank();
    }

    /* ────────────────── signUp (verifiedToken 검증) ────────────────── */

    @Test
    @DisplayName("존재하지 않는 verifiedToken으로 가입 시도 시 AUTH_EMAIL_NOT_VERIFIED 예외")
    void signUp_missingVerifiedToken_throwsException() {
        com.myfave.api.domain.auth.dto.request.SignUpRequest request =
                new com.myfave.api.domain.auth.dto.request.SignUpRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "password", "Password1!");
        ReflectionTestUtils.setField(request, "name", "테스터");
        ReflectionTestUtils.setField(request, "nickname", "tester");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");
        ReflectionTestUtils.setField(request, "verifiedToken", "no-such-token");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("signup-verified-token:no-such-token")).willReturn(null);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifiedToken의 이메일과 요청 이메일 불일치 시 AUTH_EMAIL_NOT_VERIFIED 예외")
    void signUp_emailMismatch_throwsException() {
        com.myfave.api.domain.auth.dto.request.SignUpRequest request =
                new com.myfave.api.domain.auth.dto.request.SignUpRequest();
        ReflectionTestUtils.setField(request, "email", "other@example.com");
        ReflectionTestUtils.setField(request, "password", "Password1!");
        ReflectionTestUtils.setField(request, "name", "테스터");
        ReflectionTestUtils.setField(request, "nickname", "tester");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");
        ReflectionTestUtils.setField(request, "verifiedToken", "valid-uuid");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("signup-verified-token:valid-uuid")).willReturn("test@example.com");

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효한 verifiedToken과 중복 없는 정보로 회원가입 성공")
    void signUp_success() {
        com.myfave.api.domain.auth.dto.request.SignUpRequest request =
                new com.myfave.api.domain.auth.dto.request.SignUpRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "password", "Password1!");
        ReflectionTestUtils.setField(request, "name", "테스터");
        ReflectionTestUtils.setField(request, "nickname", "tester");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");
        ReflectionTestUtils.setField(request, "verifiedToken", "valid-uuid");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("signup-verified-token:valid-uuid")).willReturn("test@example.com");
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(userRepository.existsByNickname("tester")).willReturn(false);
        given(userRepository.existsByPhone("010-1234-5678")).willReturn(false);
        given(passwordEncoder.encode("Password1!")).willReturn("encoded-password");

        com.myfave.api.domain.user.entity.User savedUser =
                com.myfave.api.domain.user.entity.User.builder()
                        .email("test@example.com")
                        .password("encoded-password")
                        .name("테스터")
                        .nickname("tester")
                        .phone("010-1234-5678")
                        .build();
        given(userRepository.save(any())).willReturn(savedUser);

        com.myfave.api.domain.auth.dto.response.SignUpResponse response = authService.signUp(request);

        assertThat(response.getNickname()).isEqualTo("tester");
        verify(redisTemplate).delete("signup-verified-token:valid-uuid");
        verify(userRepository).save(any());
    }
}
