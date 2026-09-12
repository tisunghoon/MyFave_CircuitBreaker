package com.myfave.api.global.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.core.IntervalFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

// [5막] 회복 안정화 — 서킷이 닫히자마자 다시 열리는 진동을 잡는다.
// yaml로는 지터를 못 주므로 CircuitBreakerConfigCustomizer로 pgClient 서킷을 보정한다.
// - waitDurationInOpenState 에 지터(±50%)를 줘서 여러 인스턴스의 half-open 진입이 겹치지 않게 함
// - half-open 표본 수를 올려 회복 직후 몰린 스파이크 한두 건에 성급히 재열리지 않게 함
@Configuration
@Profile("chaos")
public class Resilience4jRecoveryConfig {

    @Bean
    public CircuitBreakerConfigCustomizer pgClientRecoveryCustomizer() {
        return CircuitBreakerConfigCustomizer.of("pgClient", builder -> builder
                // 기본 10 → 20: 회복 직후 표본을 넉넉히 모아 판정 (스파이크 오염 완화)
                .permittedNumberOfCallsInHalfOpenState(20)
                // yaml waitDurationInOpenState 대신 지터를 준다 (30s 기준 ±50% 랜덤)
                .waitIntervalFunctionInOpenState(
                        IntervalFunction.ofRandomized(Duration.ofSeconds(30), 0.5))
                // half-open 최대 대기 후 자동 전이 (프로브가 안 채워져도 멈추지 않게)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(10))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED));
    }
}
