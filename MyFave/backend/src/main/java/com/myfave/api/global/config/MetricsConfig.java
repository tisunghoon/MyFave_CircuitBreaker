package com.myfave.api.global.config;

import com.myfave.api.domain.payment.entity.PaymentStatus;
import com.myfave.api.domain.payment.repository.PaymentRepository;
import com.myfave.api.domain.product.repository.ProductRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * 커스텀 비즈니스 메트릭 등록 설정.
 * Actuator + Micrometer가 기본으로 노출하지 않는 도메인 특화 지표를 정의한다.
 *
 * 노출되는 메트릭:
 * - myfave_websocket_sessions_active: 현재 활성 WebSocket 세션 수 (모든 채팅방 합산)
 * - myfave_payment_pending_gauge: 현재 PENDING 결제 건수 (Reconciliation 적체 관측)
 * - myfave_stock_snapshot: 부하테스트 시드 상품 재고 합계 (시나리오 D over-selling 검증)
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    // 부하테스트 시드 상품 ID (load-test/seed/reset.sql과 동기화 — productId BETWEEN 1 AND 10)
    private static final List<Long> LOADTEST_TARGET_PRODUCT_IDS =
            List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

    private final MeterRegistry meterRegistry;
    private final SessionRegistry sessionRegistry;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    @PostConstruct
    public void registerMetrics() {
        // WebSocket 활성 세션 수 (Gauge)
        // 모든 채팅방의 세션 수 합. 시나리오 E (라이브 채팅 1000명 동시 접속) 검증용.
        Gauge.builder("myfave.websocket.sessions.active", sessionRegistry, this::totalActiveSessions)
                .description("현재 활성 WebSocket 세션 수 (모든 채팅방 합산)")
                .register(meterRegistry);

        // PENDING 결제 적체량 — Reconciliation 정상성 + 결제 적체 알람
        Gauge.builder("myfave.payment.pending.gauge", paymentRepository, this::countPendingPayments)
                .description("현재 PENDING 상태 결제 건수")
                .register(meterRegistry);

        // 부하테스트 시드 상품 재고 합계 — 시나리오 D over-selling 검증의 SOT
        // start_stock − end_stock 이 결제 성공 카운터와 정확히 일치해야 함
        Gauge.builder("myfave.stock.snapshot", productRepository, this::sumLoadtestStock)
                .description("부하테스트 시드 상품(productId 1~10) 재고 합계")
                .register(meterRegistry);
    }

    /**
     * SessionRegistry 내 모든 채팅방의 세션 수 합계 계산.
     */
    private double totalActiveSessions(SessionRegistry registry) {
        // SessionRegistry에 getTotalSessionCount() 메서드 추가하거나 여기서 합산
        // 현재 SessionRegistry는 roomId별 카운트만 제공하므로, 합산 로직을 여기 둠
        // (불변성 유지를 위해 SessionRegistry 코드를 안 건드림)
        return registry.totalActiveSessions();
    }

    private double countPendingPayments(PaymentRepository repo) {
        return repo.countByPaymentStatus(PaymentStatus.PENDING);
    }

    private double sumLoadtestStock(ProductRepository repo) {
        return repo.sumStockQuantityByProductIds(LOADTEST_TARGET_PRODUCT_IDS);
    }
}