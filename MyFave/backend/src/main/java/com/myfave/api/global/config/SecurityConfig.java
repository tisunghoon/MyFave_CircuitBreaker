package com.myfave.api.global.config;

import com.myfave.api.global.security.JwtAuthenticationFilter;
import com.myfave.api.global.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate; // redis에 접근하기 위해서 추가

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증·문서·웹훅 등 시스템 경로 (JWT 면제)
                        .requestMatchers(
                                "/auth/**",            // context-path(/api/v1) 제외한 경로로 매칭
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/ws/**",
                                "/payments/webhook",   // 외부 PG(PortOne) 콜백 - HMAC 서명으로 자체 보안
                                "/actuator/health",
                                "/actuator/prometheus" // Prometheus scraper 접근 허용
                        ).permitAll()
                        // 비로그인 공개 조회 (카탈로그·콘텐츠·이벤트)
                        // /chat-room, /chat-room/preview, /chat-room/messages 는 의도적으로 공개:
                        // 비로그인 사용자도 판매 이벤트 전 채팅 현황을 열람할 수 있어야 함.
                        // 쓰기(WebSocket 발행)는 JWT 인증을 별도로 요구함.
                        .requestMatchers(HttpMethod.GET,
                                "/products/**",
                                "/content/short-forms",
                                "/content/style-feeds",
                                "/sale-events/current",
                                "/chat-room",
                                "/chat-room/preview",
                                "/chat-room/messages"
                        ).permitAll()
                        .anyRequest().authenticated()  // 그 외 모든 요청은 JWT 인증 필수
                )
                .exceptionHandling(ex -> ex
                        // JWT 인증 실패(토큰 없음·만료·블랙리스트) 시 Spring Security 기본값(403) 대신 401 반환
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":401,\"message\":\"인증이 필요합니다.\"}"
                            );
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security가 CORS 처리에 사용할 CorsConfigurationSource.
     * WebMvcConfigurer의 addCorsMappings는 Security 필터 체인에 자동 적용되지 않으므로
     * 명시적 Bean으로 정의하여 .cors(Customizer.withDefaults())가 이 Bean을 사용하도록 함.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "https://www.myfave.shop",
                "https://myfave.shop",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
