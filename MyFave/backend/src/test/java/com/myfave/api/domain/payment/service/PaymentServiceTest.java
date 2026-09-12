package com.myfave.api.domain.payment.service;

import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.coupon.entity.CouponMaster;
import com.myfave.api.domain.coupon.entity.CouponStatus;
import com.myfave.api.domain.coupon.entity.CouponType;
import com.myfave.api.domain.coupon.repository.CouponRepository;
import com.myfave.api.domain.coupon.service.CouponService;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import com.myfave.api.domain.order.entity.OrderStatus;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.payment.dto.request.PaymentPrepareRequest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "storeId", "store-1");
        ReflectionTestUtils.setField(paymentService, "cardChannelKey", "card-key");
        ReflectionTestUtils.setField(paymentService, "kakaoPayChannelKey", "kakao-key");
        ReflectionTestUtils.setField(paymentService, "naverPayChannelKey", "naver-key");
        ReflectionTestUtils.setField(paymentService, "tossPayChannelKey", "toss-key");
    }

    @AfterEach
    void tearDown() {}

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────
    private User buildUser(Long userId) {
        User user = User.builder()
                .email(userId + "@test.com").password("pw").name("이름" + userId)
                .nickname("nick" + userId).phone("0101234" + String.format("%04d", userId)).build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private Order buildOrder(User owner) {
        return Order.builder().user(owner).orderNumber("ORD-001").orderType(OrderType.DIRECT).build();
    }

    private Coupon buildDiscountCoupon(User owner, int discountPrice) {
        CouponMaster master = CouponMaster.builder()
                .couponName("테스트 할인쿠폰").couponType(CouponType.DISCOUNT).discountPrice(discountPrice).build();
        return Coupon.builder().couponMaster(master).user(owner).expiredAt(ZonedDateTime.now().plusDays(1)).build();
    }

    // ── preparePayment: 인증·주문 검증 ────────────────────────────────────────

    @Test
    @DisplayName("preparePayment: userId null → AUTH_UNAUTHORIZED")
    void preparePayment_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> paymentService.preparePayment(null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    @DisplayName("preparePayment: 존재하지 않는 userId → USER_NOT_FOUND")
    void preparePayment_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("preparePayment: 존재하지 않는 orderId → ORDER_NOT_FOUND")
    void preparePayment_orderNotFound_throwsOrderNotFound() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(99L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);

        verify(orderRepository).findById(99L);
    }

    @Test
    @DisplayName("preparePayment: 다른 사용자의 주문 → AUTH_FORBIDDEN")
    void preparePayment_differentUser_throwsForbidden() {
        User requester = buildUser(1L);
        User owner = buildUser(2L);
        Order order = buildOrder(owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("preparePayment: 주문 상태 PENDING 아님 → ORDER_INVALID_STATUS")
    void preparePayment_orderStatusNotPending_throwsInvalidStatus() {
        User user = buildUser(1L);
        Order order = buildOrder(user);
        order.cancel(); // CANCELLED 상태

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_INVALID_STATUS);
    }

    // ── preparePayment: 중복 결제 방지 ───────────────────────────────────────

    @Test
    @DisplayName("preparePayment: 이미 진행 중인 결제 존재 → PAYMENT_ALREADY_DONE")
    void preparePayment_existingActivePayment_throwsAlreadyDone() {
        User user = buildUser(1L);
        Order order = buildOrder(user);
        Payment existing = mock(Payment.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any()))
                .thenReturn(Optional.of(existing));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_DONE);
    }

    // ── preparePayment: 쿠폰 검증 ─────────────────────────────────────────────

    @Test
    @DisplayName("preparePayment: 존재하지 않는 할인 쿠폰 → COUPON_NOT_FOUND")
    void preparePayment_discountCouponNotFound_throwsCouponNotFound() {
        User user = buildUser(1L);
        Order order = buildOrder(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.findById(5L)).thenReturn(Optional.empty());

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(5L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    @DisplayName("preparePayment: 다른 사용자의 쿠폰 사용 시도 → AUTH_FORBIDDEN")
    void preparePayment_couponOwnedByOther_throwsForbidden() {
        User user = buildUser(1L);
        User otherUser = buildUser(2L);
        Order order = buildOrder(user);
        Coupon coupon = buildDiscountCoupon(otherUser, 1000); // 다른 사용자 소유

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.findById(5L)).thenReturn(Optional.of(coupon));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(5L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("preparePayment: 이미 사용된 쿠폰 → COUPON_ALREADY_USED")
    void preparePayment_couponAlreadyUsed_throwsCouponAlreadyUsed() {
        User user = buildUser(1L);
        Order order = buildOrder(user);
        Coupon coupon = buildDiscountCoupon(user, 1000);
        coupon.use(); // USED 상태로 변경

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.findById(5L)).thenReturn(Optional.of(coupon));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(5L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    @DisplayName("preparePayment: 만료된 쿠폰 → COUPON_EXPIRED")
    void preparePayment_couponExpired_throwsCouponExpired() {
        User user = buildUser(1L);
        Order order = buildOrder(user);
        CouponMaster master = CouponMaster.builder()
                .couponName("만료 쿠폰").couponType(CouponType.DISCOUNT).discountPrice(1000).build();
        Coupon coupon = Coupon.builder()
                .couponMaster(master).user(user)
                .expiredAt(ZonedDateTime.now().minusDays(1)) // 만료됨
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.findById(5L)).thenReturn(Optional.of(coupon));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(5L);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXPIRED);
    }

    @Test
    @DisplayName("preparePayment: 배송비 쿠폰 칸에 상품 할인 쿠폰 사용 → PAYMENT_COUPON_TYPE_MISMATCH")
    void preparePayment_couponTypeMismatch_throwsMismatch() {
        User user = buildUser(1L);
        Order order = buildOrder(user);
        CouponMaster master = CouponMaster.builder()
                .couponName("할인 쿠폰").couponType(CouponType.DISCOUNT).discountPrice(1000).build();
        Coupon coupon = Coupon.builder()
                .couponMaster(master).user(user).expiredAt(ZonedDateTime.now().plusDays(1)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.findById(5L)).thenReturn(Optional.of(coupon));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(null);
        when(req.getShippingCouponId()).thenReturn(5L); // SHIPPING 칸에 DISCOUNT 쿠폰

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_COUPON_TYPE_MISMATCH);
    }

    // ── preparePayment: 금액 검증 ─────────────────────────────────────────────

    @Test
    @DisplayName("preparePayment: 할인 후 결제 금액 음수 → PAYMENT_NEGATIVE_AMOUNT")
    void preparePayment_negativeAmount_throwsNegativeAmount() {
        User user = buildUser(1L);
        Order order = buildOrder(user);
        Coupon discountCoupon = buildDiscountCoupon(user, 50000); // 할인액 50,000원

        OrderItem item = mock(OrderItem.class);
        when(item.getPrice()).thenReturn(1000); // 상품 금액 1,000원 → 1000 + 3000 - 50000 = -46000

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.findById(5L)).thenReturn(Optional.of(discountCoupon));
        when(orderItemRepository.findByOrder(order)).thenReturn(List.of(item));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(5L);
        when(req.getShippingCouponId()).thenReturn(null);

        assertThatThrownBy(() -> paymentService.preparePayment(1L, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NEGATIVE_AMOUNT);
    }

    // ── preparePayment: 정상 흐름 ─────────────────────────────────────────────

    @Test
    @DisplayName("preparePayment: 쿠폰 없이 정상 결제 준비 → PaymentPrepareResponse 반환")
    void preparePayment_noCoupons_returnsResponse() {
        User user = buildUser(1L);
        Order order = buildOrder(user);

        OrderItem item = mock(OrderItem.class);
        when(item.getPrice()).thenReturn(10000);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderAndPaymentStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrder(order)).thenReturn(List.of(item));

        PaymentPrepareRequest req = mock(PaymentPrepareRequest.class);
        when(req.getOrderId()).thenReturn(10L);
        when(req.getDiscountCouponId()).thenReturn(null);
        when(req.getShippingCouponId()).thenReturn(null);
        when(req.getPaymentMethod()).thenReturn(PaymentMethod.CARD);

        var response = paymentService.preparePayment(1L, req);

        assertThat(response).isNotNull();
        assertThat(response.getTotalPaymentPrice()).isEqualTo(13000); // 10000 + 3000 - 0
        assertThat(response.getDeliveryFee()).isEqualTo(3000);
        verify(paymentRepository).save(any(Payment.class));
    }
}
