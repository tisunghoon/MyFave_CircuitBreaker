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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceConfirmTest {

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

    private Payment buildPendingPayment(User owner) {
        Order order = Order.builder().user(owner).orderNumber("ORD-001").orderType(OrderType.DIRECT).build();
        return Payment.builder()
                .order(order).idempotencyKey("idem-key").pgProvider("PORTONE")
                .paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(10000).deliveryFee(3000).discountPrice(0).totalPaymentPrice(13000)
                .build();
    }

    private PortOnePaymentInfo pgInfo(int totalAmount) {
        return new PortOnePaymentInfo("pg-tx-1", "PAID", totalAmount, "receipt-url", ZonedDateTime.now());
    }

    // ── validateForConfirm ────────────────────────────────────────────────────

    @Test
    @DisplayName("validateForConfirm: 결제 미존재 → PAYMENT_NOT_FOUND")
    void validateForConfirm_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.validateForConfirm(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("validateForConfirm: 다른 사용자의 결제 → AUTH_FORBIDDEN")
    void validateForConfirm_differentUser_throwsForbidden() {
        User owner = buildUser(2L);
        Payment payment = buildPendingPayment(owner);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.validateForConfirm(1L, 1L)) // userId=1, owner=2
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("validateForConfirm: COMPLETED 상태 결제 → PAYMENT_INVALID_STATUS")
    void validateForConfirm_completedStatus_throwsInvalidStatus() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner);
        payment.authorize("pg-tx");
        payment.complete("url", ZonedDateTime.now()); // COMPLETED

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.validateForConfirm(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_INVALID_STATUS);
    }

    @Test
    @DisplayName("validateForConfirm: PENDING 상태 → ConfirmContext 반환")
    void validateForConfirm_pendingStatus_returnsContext() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner);
        ReflectionTestUtils.setField(payment, "paymentId", 1L);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentService.ConfirmContext ctx = paymentService.validateForConfirm(1L, 1L);

        assertThat(ctx.paymentId()).isEqualTo(1L);
        assertThat(ctx.totalPaymentPrice()).isEqualTo(13000);
    }

    @Test
    @DisplayName("validateForConfirm: AUTHORIZED 상태도 승인 가능 → ConfirmContext 반환")
    void validateForConfirm_authorizedStatus_returnsContext() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner);
        payment.authorize("pg-tx"); // AUTHORIZED
        ReflectionTestUtils.setField(payment, "paymentId", 1L);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentService.ConfirmContext ctx = paymentService.validateForConfirm(1L, 1L);

        assertThat(ctx.paymentId()).isEqualTo(1L);
    }

    // ── completeConfirm ───────────────────────────────────────────────────────

    @Test
    @DisplayName("completeConfirm: 결제 미존재 → PAYMENT_NOT_FOUND")
    void completeConfirm_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.completeConfirm(1L, pgInfo(13000), 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("completeConfirm: 쿠폰 없이 결제 완료 → 상태 COMPLETED, 주문 PAID")
    void completeConfirm_noCoupons_completesPaymentAndOrder() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        var response = paymentService.completeConfirm(1L, pgInfo(13000), 1L);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response).isNotNull();
        verify(paymentAttemptRepository).save(any());
    }

    @Test
    @DisplayName("completeConfirm: 할인 쿠폰 있을 때 → couponService.useCoupon 호출")
    void completeConfirm_withDiscountCoupon_usesCoupon() {
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

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        paymentService.completeConfirm(1L, pgInfo(12000), 1L);

        verify(couponService).useCoupon(5L, 1L);
    }

    // ── failConfirm ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("failConfirm: 결제 미존재 → PAYMENT_NOT_FOUND")
    void failConfirm_paymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.failConfirm(1L, "pg-tx", "실패 사유"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("failConfirm: 결제 실패 처리 → 상태 FAILED, 이력 저장")
    void failConfirm_setsFailedStatusAndSavesAttempt() {
        User owner = buildUser(1L);
        Payment payment = buildPendingPayment(owner);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentPaymentId(1L)).thenReturn(0);

        paymentService.failConfirm(1L, "pg-tx", "금액 불일치");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentAttemptRepository).save(any());
    }

    // ── confirmPayment: 인증 검사 (NOT_SUPPORTED 오케스트레이터) ──────────────

    @Test
    @DisplayName("confirmPayment: userId null → AUTH_UNAUTHORIZED")
    void confirmPayment_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> paymentService.confirmPayment(null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }
}
