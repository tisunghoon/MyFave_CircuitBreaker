package com.myfave.api.domain.payment.service;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.entity.PaymentStatus;
import com.myfave.api.domain.payment.repository.PaymentRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentService.findPendingReconcileTargets 의 쿼리 수 측정.
 * 현재 구현은 Payment만 SELECT → ReconcileTarget(record)로 매핑이므로 1쿼리 예상.
 * 만약 루프에서 payment.getOrder().getUser() 같은 LAZY 접근이 추가되면 batch_fetch_size에 따라
 * 추가 쿼리(N/100 ceil)가 발생함을 함께 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentReconcileN1Test {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    @BeforeEach
    void syncSequences() {
        syncSeq("users", "user_id");
        syncSeq("orders", "orders_id");
        syncSeq("payments", "payment_id");
    }

    private void syncSeq(String table, String col) {
        em.createNativeQuery(
                "SELECT setval(pg_get_serial_sequence('" + table + "', '" + col + "'), " +
                "GREATEST(COALESCE((SELECT MAX(" + col + ") FROM " + table + "), 0), 1))"
        ).getSingleResult();
    }

    @DisplayName("findPendingReconcileTargets: PENDING Payment N개 조회 시 쿼리 수 baseline")
    @ParameterizedTest(name = "PENDING {0}건 → 쿼리 수 측정")
    @ValueSource(ints = {5, 20, 50})
    void findPendingReconcileTargets_baselineQueryCount(int paymentCount) {
        User user = persistUser();
        for (int i = 0; i < paymentCount; i++) {
            Order order = persistOrder(user, i);
            persistPendingPayment(order, i);
        }
        em.flush();
        em.clear();

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        // threshold를 미래로 잡아 모든 PENDING이 대상이 되게 함
        ZonedDateTime threshold = ZonedDateTime.now().plusHours(1);

        long start = System.nanoTime();
        List<PaymentService.ReconcileTarget> targets = paymentService.findPendingReconcileTargets(threshold);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        long queryCount = stats.getPrepareStatementCount();

        System.out.printf("[reconcile baseline] payments=%d → queries=%d, elapsed=%dms%n",
                paymentCount, queryCount, elapsedMs);

        // 시퀀스 동기화/threshold 조건 등으로 targets >= paymentCount (다른 PENDING 누적 가능)
        assertThat(targets.size()).isGreaterThanOrEqualTo(paymentCount);
        // ReconcileTarget은 LAZY 연관관계를 건드리지 않으므로 단일 SELECT 기대
        assertThat(queryCount).isLessThanOrEqualTo(2);
    }

    @DisplayName("payment.getOrder() Lazy 접근 시 batch_fetch_size=100 효과 확인")
    @ParameterizedTest(name = "PENDING {0}건 → Order 접근 시 쿼리 수")
    @ValueSource(ints = {5, 50, 150})
    void lazyOrderAccess_isBatchedByHibernateFetchSize(int paymentCount) {
        User user = persistUser();
        for (int i = 0; i < paymentCount; i++) {
            Order order = persistOrder(user, i);
            persistPendingPayment(order, i);
        }
        em.flush();
        em.clear();

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        // Payment 전체 로드 (1쿼리) → 각 payment.getOrder() 접근 (batch_fetch_size=100으로 묶임)
        List<Payment> payments = paymentRepository
                .findByPaymentStatusAndCreatedAtBefore(PaymentStatus.PENDING, ZonedDateTime.now().plusHours(1));

        for (Payment p : payments) {
            // LAZY → 첫 접근 시 batch 로딩 (최대 100개씩 IN 쿼리)
            p.getOrder().getOrderNumber();
        }

        long queryCount = stats.getPrepareStatementCount();
        int expectedOrderBatches = (int) Math.ceil(paymentCount / 100.0);
        long expectedTotal = 1L + expectedOrderBatches; // Payment 1쿼리 + Order 배치 N쿼리

        System.out.printf("[batch_fetch_size 효과] payments=%d → queries=%d (Payment 1 + Order 배치 %d)%n",
                paymentCount, queryCount, expectedOrderBatches);

        assertThat(queryCount).isEqualTo(expectedTotal);
    }

    // ─── 픽스처 헬퍼 ─────────────────────────────────────────────────────────
    private static long seq = System.nanoTime();

    private User persistUser() {
        long s = seq++;
        // nickname: length=12 제약
        String nick = "rc" + (s % 1_000_000_000L); // 최대 11자
        User user = User.builder()
                .email("rec-" + s + "@example.com")
                .password("password!")
                .name("RecTester")
                .nickname(nick)
                .phone("011-" + String.format("%04d", s % 10000) + "-" + String.format("%04d", (s / 10000) % 10000))
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Order persistOrder(User user, int idx) {
        long s = seq++;
        Order order = Order.builder()
                .user(user)
                .orderNumber("ORD-REC-" + s + "-" + idx)
                .orderType(OrderType.DIRECT)
                .build();
        return orderRepository.saveAndFlush(order);
    }

    private Payment persistPendingPayment(Order order, int idx) {
        long s = seq++;
        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey("idem-rec-" + s + "-" + idx)
                .pgProvider("portone")
                .totalProductPrice(10_000)
                .deliveryFee(3_000)
                .discountPrice(0)
                .totalPaymentPrice(13_000)
                .build();
        return paymentRepository.saveAndFlush(payment);
    }
}
