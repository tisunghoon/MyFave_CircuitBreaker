package com.myfave.api.domain.payment.service;

import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.coupon.entity.CouponMaster;
import com.myfave.api.domain.coupon.entity.CouponType;
import com.myfave.api.domain.coupon.repository.CouponRepository;
import com.myfave.api.domain.coupon.service.CouponService;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderStatus;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.payment.dto.request.PaymentCancelRequest;
import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.entity.PaymentMethod;
import com.myfave.api.domain.payment.entity.PaymentStatus;
import com.myfave.api.domain.payment.provider.PaymentProvider;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceCancelTest {

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

    private Payment buildCompletedPayment(User owner, int totalPaymentPrice) {
        Order order = Order.builder().user(owner).orderNumber("ORD-001").orderType(OrderType.DIRECT).build();
        Payment payment = Payment.builder()
                .order(order).idempotencyKey("idem-key").pgProvider("PORTONE")
                .paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(totalPaymentPrice - 3000).deliveryFee(3000).discountPrice(0)
                .totalPaymentPrice(totalPaymentPrice)
                .build();
        payment.authorize("pg-tx-1");
        payment.complete("receipt-url", ZonedDateTime.now());
        return payment;
    }

    // ── getPayment ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPayment: userId null → AUTH_UNAUTHORIZED")
    void getPayment_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> paymentService.getPayment(null, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    @DisplayName("getPayment: 결제 미존재 → PAYMENT_NOT_FOUND")
    void getPayment_notFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("getPayment: 다른 사용자 결제 조회 → AUTH_FORBIDDEN")
    void getPayment_differentUser_throwsForbidden() {
        User owner = buildUser(2L);
        Payment payment = buildCompletedPayment(owner, 13000);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPayment(1L, 1L)) // userId=1, owner=2
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("getPayment: 정상 조회 → PaymentResponse 반환")
    void getPayment_success() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        var response = paymentService.getPayment(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    // ── validateForCancel ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateForCancel: 결제 미존재 → PAYMENT_NOT_FOUND")
    void validateForCancel_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);

        assertThatThrownBy(() -> paymentService.validateForCancel(1L, 1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("validateForCancel: 다른 사용자 결제 → AUTH_FORBIDDEN")
    void validateForCancel_differentUser_throwsForbidden() {
        User owner = buildUser(2L);
        Payment payment = buildCompletedPayment(owner, 13000);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);

        assertThatThrownBy(() -> paymentService.validateForCancel(1L, 1L, req)) // userId=1, owner=2
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("validateForCancel: CANCELLED 상태 → PAYMENT_CANCELLED")
    void validateForCancel_alreadyCancelled_throwsCancelled() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);
        payment.cancel(); // CANCELLED

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);

        assertThatThrownBy(() -> paymentService.validateForCancel(1L, 1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_CANCELLED);
    }

    @Test
    @DisplayName("validateForCancel: PENDING 상태 → PAYMENT_INVALID_STATUS")
    void validateForCancel_pendingStatus_throwsInvalidStatus() {
        User owner = buildUser(1L);
        Order order = Order.builder().user(owner).orderNumber("ORD-001").orderType(OrderType.DIRECT).build();
        Payment payment = Payment.builder()
                .order(order).idempotencyKey("key").pgProvider("PORTONE")
                .paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(10000).deliveryFee(3000).discountPrice(0).totalPaymentPrice(13000)
                .build(); // PENDING 상태

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);

        assertThatThrownBy(() -> paymentService.validateForCancel(1L, 1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_INVALID_STATUS);
    }

    @Test
    @DisplayName("validateForCancel: 남은 환불 금액 0원 → PAYMENT_INVALID_STATUS")
    void validateForCancel_zeroRemainingAmount_throwsInvalidStatus() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);
        payment.partialCancel(13000); // 전액 환불 상태 (PARTIAL_CANCELLED, refundedAmount=13000)
        // remaining = 13000 - 13000 = 0 → cancelAmount = 0

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);
        when(req.getRefundAmount()).thenReturn(null); // 전체 취소 시도

        assertThatThrownBy(() -> paymentService.validateForCancel(1L, 1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_INVALID_STATUS);
    }

    @Test
    @DisplayName("validateForCancel: refundAmount null → 전체 취소, CancelContext 반환")
    void validateForCancel_fullCancel_returnsContext() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);
        ReflectionTestUtils.setField(payment, "paymentId", 1L);
        ReflectionTestUtils.setField(payment, "pgTransactionId", "pg-tx-1");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);
        when(req.getRefundAmount()).thenReturn(null); // 전체 취소

        PaymentService.CancelContext ctx = paymentService.validateForCancel(1L, 1L, req);

        assertThat(ctx.cancelAmount()).isEqualTo(13000);
        assertThat(ctx.fullCancel()).isTrue();
    }

    @Test
    @DisplayName("validateForCancel: refundAmount 지정 → 부분 취소, CancelContext 반환")
    void validateForCancel_partialCancel_returnsContext() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);
        ReflectionTestUtils.setField(payment, "paymentId", 1L);
        ReflectionTestUtils.setField(payment, "pgTransactionId", "pg-tx-1");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelRequest req = mock(PaymentCancelRequest.class);
        when(req.getRefundAmount()).thenReturn(5000); // 부분 취소

        PaymentService.CancelContext ctx = paymentService.validateForCancel(1L, 1L, req);

        assertThat(ctx.cancelAmount()).isEqualTo(5000);
        assertThat(ctx.fullCancel()).isFalse();
    }

    // ── applyCancelResult ─────────────────────────────────────────────────────

    @Test
    @DisplayName("applyCancelResult: 결제 미존재 → PAYMENT_NOT_FOUND")
    void applyCancelResult_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.applyCancelResult(1L, 13000, true, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("applyCancelResult: 전체 취소, 쿠폰 없음 → 상태 CANCELLED, 주문 REFUNDED")
    void applyCancelResult_fullCancel_noCoupons() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        var response = paymentService.applyCancelResult(1L, 13000, true, 1L);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getOrder().getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("applyCancelResult: 전체 취소, 할인 쿠폰 있음 → couponService.restoreCoupon 호출")
    void applyCancelResult_fullCancel_withDiscountCoupon_restoresCoupon() {
        User owner = buildUser(1L);
        CouponMaster master = CouponMaster.builder()
                .couponName("할인").couponType(CouponType.DISCOUNT).discountPrice(1000).build();
        Coupon discountCoupon = Coupon.builder()
                .couponMaster(master).user(owner).expiredAt(ZonedDateTime.now().plusDays(1)).build();
        ReflectionTestUtils.setField(discountCoupon, "couponId", 5L);

        Order order = Order.builder().user(owner).orderNumber("ORD-002").orderType(OrderType.DIRECT).build();
        Payment payment = Payment.builder()
                .order(order).discountCoupon(discountCoupon).idempotencyKey("key2")
                .pgProvider("PORTONE").paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(10000).deliveryFee(3000).discountPrice(1000).totalPaymentPrice(12000)
                .build();
        payment.authorize("pg-tx");
        payment.complete("receipt", ZonedDateTime.now());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.applyCancelResult(1L, 12000, true, 1L);

        verify(couponService).restoreCoupon(5L, 1L);
    }

    @Test
    @DisplayName("applyCancelResult: 부분 취소 → 상태 PARTIAL_CANCELLED")
    void applyCancelResult_partialCancel() {
        User owner = buildUser(1L);
        Payment payment = buildCompletedPayment(owner, 13000);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        var response = paymentService.applyCancelResult(1L, 5000, false, 1L);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELLED);
        assertThat(payment.getRefundedAmount()).isEqualTo(5000);
        assertThat(response).isNotNull();
    }

    // ── cancelPayment: 인증 검사 (NOT_SUPPORTED 오케스트레이터) ──────────────

    @Test
    @DisplayName("cancelPayment: userId null → AUTH_UNAUTHORIZED")
    void cancelPayment_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> paymentService.cancelPayment(null, 1L, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }
}
