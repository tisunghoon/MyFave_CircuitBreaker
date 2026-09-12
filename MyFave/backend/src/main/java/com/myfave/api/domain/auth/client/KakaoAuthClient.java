package com.myfave.api.domain.auth.client;

import com.myfave.api.domain.auth.client.dto.KakaoTokenResponse;
import com.myfave.api.domain.auth.client.dto.KakaoUserInfoResponse;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private WebClient kakaoAuthWebClient;
    private WebClient kakaoApiWebClient;

    @PostConstruct
    private void init() {
        kakaoAuthWebClient = webClientBuilder.baseUrl("https://kauth.kakao.com").build();
        kakaoApiWebClient = WebClient.builder().baseUrl("https://kapi.kakao.com").build();
    }

    public KakaoTokenResponse exchangeCodeForToken(String authorizationCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", authorizationCode);

        return kakaoAuthWebClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        resp -> Mono.error(new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_CODE)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        resp -> Mono.error(new CustomException(ErrorCode.AUTH_SOCIAL_PROVIDER_ERROR)))
                .bodyToMono(KakaoTokenResponse.class)
                .onErrorMap(WebClientRequestException.class,
                        e -> new CustomException(ErrorCode.AUTH_SOCIAL_PROVIDER_ERROR))
                .block(Duration.ofSeconds(5));
    }

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        return kakaoApiWebClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        resp -> Mono.error(new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_CODE)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        resp -> Mono.error(new CustomException(ErrorCode.AUTH_SOCIAL_PROVIDER_ERROR)))
                .bodyToMono(KakaoUserInfoResponse.class)
                .onErrorMap(WebClientRequestException.class,
                        e -> new CustomException(ErrorCode.AUTH_SOCIAL_PROVIDER_ERROR))
                .block(Duration.ofSeconds(5));
    }
}
