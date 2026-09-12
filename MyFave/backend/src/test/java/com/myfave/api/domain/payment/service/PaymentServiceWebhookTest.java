package com.myfave.api.domain.payment.service;

import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.coupon.entity.CouponMaster;
import com.myfave.api.domain.coupon.entity.CouponType;
import com.myfave.api.domain.coupon.repository.CouponRepository;
import com.myfave.api.domain.coupon.service.CouponService;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.entity.PaymentMethod;
import com.myfave.api.domain.payment.entity.PaymentStatus;
import com.myfave.api.domain.payment.provider.PaymentProvider;
import com.myfave.api.domain.payment.provider.PaymentProvider.PortOnePaymentInfo;
import com.myfave.api.domain.payment.repository.PaymentAttemptRepository;
import com.myfave.api.domain.payment.repository.PaymentRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceWebhookTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private CouponService couponService;
    @Mock private UserRepository userRepository;
    @Mock private PaymentProvider paymentProvider;
    @Mock private PaymentService self;

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────
    private User buildUser(Long userId) {
        User user = User.builder()
                .email(userId + "@test.com").password("pw").name("이름")
                .nickname("nick" + userId).phone("0101234" + String.format("%04d", userId)).build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private Payment buildPendingPayment(User owner, int totalPaymentPrice) {
        Order order = Order.builder().user(owner).orderNumber("ORD-001").orderType(OrderType.DIRECT).build();
        return Payment.builder()
                .order(order).idempotencyKey("idem-key").pgProvider("PORTONE")
                .paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(totalPaymentPrice - 3000).deliveryFee(3000).discountPrice(0)
                .totalPaymentPrice(totalPaymentPrice)
                .build();
    }

    private PortOnePaymentInfo pgInfo(String status, int totalAmount) {
        return new PortOnePaymentInfo("pg-tx-1", status, totalAmount, "receipt-url", ZonedDateTime.now());
    }

    // ── loadForWebhook ────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadForWebhook: pgTransactionId 미존재 → null 반환")
    void loadForWebhook_notFound_returnsNull() {
        when(paymentRepository.findByPgTransactionId("pg-tx")).thenReturn(Optional.empty());

        PaymentService.WebhookContext ctx = paymentService.loadForWebhook("pg-tx");

        assertThat(ctx).isNull();
    }

    @Test
    @DisplayName("loadForWebhook: 결제 존재 → WebhookContext 반환")
    void loadForWebhook_found_returnsContext() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        ReflectionTestUtils.setField(payment, "paymentId", 1L);

        when(paymentRepository.findByPgTransactionId("pg-tx")).thenReturn(Optional.of(payment));

        PaymentService.WebhookContext ctx = paymentService.loadForWebhook("pg-tx");

        assertThat(ctx).isNotNull();
        assertThat(ctx.paymentId()).isEqualTo(1L);
        assertThat(ctx.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(ctx.totalPaymentPrice()).isEqualTo(13000);
    }

    // ── completeWebhook ───────────────────────────────────────────────────────

    @Test
    @DisplayName("completeWebhook: 결제 미존재 → PAYMENT_NOT_FOUND")
    void completeWebhook_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.completeWebhook(1L, pgInfo("PAID", 13000)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("completeWebhook: 쿠폰 없이 완료 → 상태 COMPLETED, 주문 PAID")
    void completeWebhook_noCoupons_completesPayment() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        paymentService.completeWebhook(1L, pgInfo("PAID", 13000));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentAttemptRepository).save(any());
    }

    @Test
    @DisplayName("completeWebhook: 할인 쿠폰 있을 때 → couponService.useCoupon 호출")
    void completeWebhook_withDiscountCoupon_usesCoupon() {
        User owner = buildUser(1L);
        CouponMaster master = CouponMaster.builder()
                .couponName("할인").couponType(CouponType.DISCOUNT).discountPrice(1000).build();
        Coupon discountCoupon = Coupon.builder()
                .couponMaster(master).user(owner).expiredAt(ZonedDateTime.now().plusDays(1)).build();
        ReflectionTestUtils.setField(discountCoupon, "couponId", 5L);

        Order order = Order.builder().user(owner).orderNumber("ORD-002").orderType(OrderType.DIRECT).build();
        Payment payment = Payment.builder()
                .order(order).discountCoupon(discountCoupon).idempotencyKey("key")
                .pgProvider("PORTONE").paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(10000).deliveryFee(3000).discountPrice(1000).totalPaymentPrice(12000)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        paymentService.completeWebhook(1L, pgInfo("PAID", 12000));

        verify(couponService).useCoupon(5L, 1L);
    }

    // ── failWebhook ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("failWebhook: 결제 미존재 → PAYMENT_NOT_FOUND")
    void failWebhook_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.failWebhook(1L, "pg-tx", "실패"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("failWebhook: 실패 처리 → 상태 FAILED, 이력 저장")
    void failWebhook_setsFailedStatus() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        paymentService.failWebhook(1L, "pg-tx", "웹훅 금액 불일치");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentAttemptRepository).save(any());
    }

    // ── recordWebhookFailed ───────────────────────────────────────────────────

    @Test
    @DisplayName("recordWebhookFailed: 결제 미존재 → PAYMENT_NOT_FOUND")
    void recordWebhookFailed_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.recordWebhookFailed(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("recordWebhookFailed: PG 결제 실패 → 상태 FAILED")
    void recordWebhookFailed_setsFailedStatus() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.recordWebhookFailed(1L);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailReason()).isEqualTo("웹훅: PG 결제 실패");
    }

    // ── findPendingReconcileTargets ───────────────────────────────────────────

    @Test
    @DisplayName("findPendingReconcileTargets: PENDING 결제 목록 → ReconcileTarget 리스트 반환")
    void findPendingReconcileTargets_returnsTargets() {
        User owner = buildUser(1L);
        Payment p1 = buildPendingPayment(owner, 10000);
        Payment p2 = buildPendingPayment(owner, 20000);
        ReflectionTestUtils.setField(p1, "paymentId", 1L);
        ReflectionTestUtils.setField(p2, "paymentId", 2L);

        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(30);
        when(paymentRepository.findByPaymentStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold))
                .thenReturn(List.of(p1, p2));

        List<PaymentService.ReconcileTarget> targets = paymentService.findPendingReconcileTargets(threshold);

        assertThat(targets).hasSize(2);
        assertThat(targets.get(0).paymentId()).isEqualTo(1L);
        assertThat(targets.get(0).totalPaymentPrice()).isEqualTo(10000);
    }

    // ── reconcileOne ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("reconcileOne: 결제 미존재 → PAYMENT_NOT_FOUND")
    void reconcileOne_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.reconcileOne(1L, 13000, pgInfo("PAID", 13000)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("reconcileOne: PAID + 금액 일치 → 결제 COMPLETED 처리")
    void reconcileOne_paidAndMatchingAmount_completesPayment() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        paymentService.reconcileOne(1L, 13000, pgInfo("PAID", 13000));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentAttemptRepository).save(any());
    }

    @Test
    @DisplayName("reconcileOne: PAID + 금액 불일치 → 아무 상태 변경 없음")
    void reconcileOne_paidButAmountMismatch_doesNotComplete() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.reconcileOne(1L, 13000, pgInfo("PAID", 99999)); // 금액 불일치

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING); // 변경 없음
    }

    @Test
    @DisplayName("reconcileOne: PG 상태 FAILED → 결제 FAILED 처리")
    void reconcileOne_failedStatus_setsFailure() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.reconcileOne(1L, 13000, pgInfo("FAILED", 0));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("reconcileOne: PG 상태 CANCELLED → 결제 FAILED 처리")
    void reconcileOne_cancelledStatus_setsFailure() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.reconcileOne(1L, 13000, pgInfo("CANCELLED", 0));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
