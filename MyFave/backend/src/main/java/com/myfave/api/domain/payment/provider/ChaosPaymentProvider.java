package com.myfave.api.domain.payment.provider;

import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.repository.PaymentRepository;
import com.myfave.api.global.chaos.ChaosProperties;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

// [DRILL] 장애 주입용 PaymentProvider. @Profile("chaos") 일 때만 @Primary 로 실 provider 대신 사용.
// 핵심 = 느린 성공(HTTP 200 이 latencyMs 뒤). 에러가 아니다 → 3막 "느린 응답은 실패로 안 잡힌다".
// pgTransactionId 패턴 MOCK-PAY-{paymentId} 로 실제 결제 금액을 DB 조회해 confirm 금액검증 통과.
@Slf4j
@Component
@Profile("chaos")
@Primary
@RequiredArgsConstructor
public class ChaosPaymentProvider implements PaymentProvider {

    private static final String MOCK_PREFIX = "MOCK-PAY-";

    private final PaymentRepository paymentRepository;
    private final ChaosProperties chaos;
    private final MeterRegistry meterRegistry;

    @Override
    @CircuitBreaker(name = "pgClient")   // [2막] 기본값 서킷브레이커 — 활성 provider(chaos)에서 관측
    @RateLimiter(name = "pgClient")      // [5막] 회복 직후 스파이크 유량 제한
    public PortOnePaymentInfo getPaymentInfo(String pgTransactionId) {
        return invokeWithMetrics("getPaymentInfo", () -> {
            injectLatencyOrError();
            int amount = resolveAmount(pgTransactionId);
            return new PortOnePaymentInfo(
                    pgTransactionId, "PAID", amount,
                    "https://chaos.receipt/" + pgTransactionId, ZonedDateTime.now());
        });
    }

    @Override
    public void cancelPayment(String pgTransactionId, int cancelAmount, String reason) {
        invokeWithMetrics("cancelPayment", () -> {
            injectLatencyOrError();
            log.debug("[Chaos] cancel pgTxId={}, amount={}, reason={}", pgTransactionId, cancelAmount, reason);
            return null;
        });
    }

    // 실 provider 호출 직전 지연/에러 주입.
    private void injectLatencyOrError() {
        if (!chaos.isEnabled()) return;
        if (chaos.getMode() == ChaosProperties.Mode.ERROR) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
        try {
            Thread.sleep(chaos.getLatencyMs());   // 느린 성공 — 스레드/커넥션 점유
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int resolveAmount(String pgTransactionId) {
        if (pgTransactionId == null || !pgTransactionId.startsWith(MOCK_PREFIX)) {
            return 13000;
        }
        try {
            Long paymentId = Long.parseLong(pgTransactionId.substring(MOCK_PREFIX.length()));
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
            return payment.getTotalPaymentPrice();
        } catch (NumberFormatException e) {
            return 13000;
        }
    }

    // 실 provider 와 동일한 타이머 이름으로 지연 분포 기록 (myfave.portone.call.duration).
    private <T> T invokeWithMetrics(String api, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        try {
            T result = call.get();
            outcome = "success";
            return result;
        } finally {
            sample.stop(Timer.builder("myfave.portone.call.duration")
                    .tag("api", api).tag("outcome", outcome).register(meterRegistry));
        }
    }
}
