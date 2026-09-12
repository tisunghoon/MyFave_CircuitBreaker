package com.myfave.api.global.config;

import com.myfave.api.domain.cart.entity.CartItem;
import com.myfave.api.domain.cart.repository.CartItemRepository;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.entity.PaymentMethod;
import com.myfave.api.domain.payment.repository.PaymentRepository;
import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.ConditionCode;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.repository.ProductRepository;
import com.myfave.api.domain.shipping.entity.Delivery;
import com.myfave.api.domain.shipping.entity.ShippingAddress;
import com.myfave.api.domain.shipping.repository.DeliveryRepository;
import com.myfave.api.domain.shipping.repository.ShippingAddressRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// @Profile("local"): 로컬 개발 환경에서만 실행 (prod 환경에서는 동작 안 함)
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final PasswordEncoder passwordEncoder;

    // CommandLineRunner: 앱 시작 완료 직후 자동 실행
    @Override
    @Transactional
    public void run(String... args) {

        // 이미 테스트 데이터가 있으면 중복 삽입하지 않음
        if (userRepository.existsById(1L)) {
            log.info("[DataInitializer] 테스트 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }

        // ── 1. 테스트 유저 생성 ─────────────────────────────────────
        // Order API 테스트 시 userId=1 로 자동 설정됨 (JWT 없을 때)
        User user = User.builder()
                .email("test@test.com")
                .password(passwordEncoder.encode("password"))
                .name("테스트유저")
                .nickname("tester")
                .phone("010-0000-0000")
                .build();
        userRepository.save(user);
        log.info("[DataInitializer] 테스트 유저 생성 완료 (id={})", user.getUserId());

        // ── 2. 테스트 상품 생성 ─────────────────────────────────────
        // DIRECT 주문 테스트: productId=1 사용
        Product product = Product.builder()
                .user(user)
                .productName("테스트 상품")
                .shortReview("테스트용 상품입니다.")
                .price(10000)
                .description("Order API 테스트를 위한 더미 상품")
                .size("FREE")
                .conditionCode(ConditionCode.S_GRADE)
                .categoryCode(CategoryCode.TOP)
                .build();
        productRepository.save(product);
        log.info("[DataInitializer] 테스트 상품 생성 완료 (id={})", product.getProductId());

        // ── 3. 테스트 배송지 생성 ───────────────────────────────────
        // 주문 요청 시 shippingAddressId=1 사용
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .user(user)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .address("서울시 강남구 역삼동 123")
                .addressDetail("101호")
                .zipCode("12345")
                .deliveryRequest("문 앞에 놓아주세요")
                .isDefault(true)
                .build();
        shippingAddressRepository.save(shippingAddress);
        log.info("[DataInitializer] 테스트 배송지 생성 완료 (id={})", shippingAddress.getShippingId());

        // ── 4. 테스트 장바구니 항목 생성 ────────────────────────────
        // CART 주문 테스트: cartItemIds=[1] 사용
        CartItem cartItem = CartItem.builder()
                .user(user)
                .product(product)
                .build();
        cartItemRepository.save(cartItem);
        log.info("[DataInitializer] 테스트 장바구니 항목 생성 완료 (id={})", cartItem.getCartId());

        log.info("[DataInitializer] Order API 기본 테스트 데이터 삽입 완료");

        // ── 5-3 테스트용 Order, OrderItem, Payment, Delivery 생성 ────
        // 5-3 상세 조회 테스트 시 orderId=1 사용
        createOrderDetailTestData(user, product);
    }

    // 5-3 주문 상세 조회 테스트용 데이터 생성
    // 이미 존재하면 건너뜀 (중복 방지)
    private void createOrderDetailTestData(User user, Product product) {

        if (orderRepository.findByOrderNumber("ORD-20260407-TEST001").isPresent()) {
            log.info("[DataInitializer] 5-3 테스트 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }

        // ── 5-1. Order 생성 ─────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .orderNumber("ORD-20260407-TEST001")
                .orderType(OrderType.DIRECT)
                .build();
        orderRepository.save(order);
        log.info("[DataInitializer] 테스트 주문 생성 완료 (id={})", order.getOrderId());

        // ── 5-2. OrderItem 생성 ──────────────────────────────────────
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .price(product.getPrice())
                .productName(product.getProductName())
                .build();
        orderItemRepository.save(orderItem);
        log.info("[DataInitializer] 테스트 주문 항목 생성 완료 (id={})", orderItem.getOrderItemId());

        // ── 5-3. Payment 생성 ────────────────────────────────────────
        // Payment 도메인 미완성 → 테스트용 더미 결제 데이터
        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey(java.util.UUID.randomUUID().toString())
                .pgProvider("PORTONE")
                .paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(10000)
                .deliveryFee(3000)
                .discountPrice(0)
                .totalPaymentPrice(13000)
                .build();
        paymentRepository.save(payment);
        log.info("[DataInitializer] 테스트 결제 생성 완료 (id={})", payment.getPaymentId());

        // ── 5-4. Order에 finalPayment 연결 + 상태 PAID로 변경 ────────
        // completePay(): finalPayment 세팅 + orderStatus = PAID
        order.completePay(payment);
        orderRepository.save(order);

        // ── 5-5. Delivery 생성 ───────────────────────────────────────
        Delivery delivery = Delivery.builder()
                .order(order)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울시 강남구 역삼동 123")
                .deliveryRequest("문 앞에 놓아주세요")
                .build();
        deliveryRepository.save(delivery);

        // ship(): courierName, trackingNumber 세팅 + deliveryStatus = SHIPPING
        delivery.ship("CJ대한통운", "kr.cjlogistics", "1234567890");
        deliveryRepository.save(delivery);
        log.info("[DataInitializer] 테스트 배송 생성 완료 (id={})", delivery.getDeliveryId());

        log.info("[DataInitializer] 5-3 테스트 데이터 삽입 완료 — orderId={} 로 상세 조회 테스트 가능", order.getOrderId());

        // ── 5-4 테스트용 Order 생성 (DELIVERY_COMPLETED 상태) ────────────
        createConfirmTestData(user, product);
    }

    // 5-4 구매확정 테스트용 데이터 생성
    // DELIVERY_COMPLETED 상태의 주문 → PATCH /orders/{orderId}/confirm 테스트에 사용
    private void createConfirmTestData(User user, Product product) {

        if (orderRepository.findByOrderNumber("ORD-20260407-TEST002").isPresent()) {
            log.info("[DataInitializer] 5-4 테스트 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }

        // ── 6-1. Order 생성 ─────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .orderNumber("ORD-20260407-TEST002")
                .orderType(OrderType.DIRECT)
                .build();
        orderRepository.save(order);

        // ── 6-2. OrderItem 생성 ──────────────────────────────────────
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .price(product.getPrice())
                .productName(product.getProductName())
                .build();
        orderItemRepository.save(orderItem);

        // ── 6-3. Payment 생성 + Order에 연결 ────────────────────────
        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey(java.util.UUID.randomUUID().toString())
                .pgProvider("PORTONE")
                .paymentMethod(PaymentMethod.CARD)
                .totalProductPrice(10000)
                .deliveryFee(3000)
                .discountPrice(0)
                .totalPaymentPrice(13000)
                .build();
        paymentRepository.save(payment);
        order.completePay(payment); // orderStatus = PAID
        orderRepository.save(order);

        // ── 6-4. Delivery 생성 + 배송완료 처리 ──────────────────────
        Delivery delivery = Delivery.builder()
                .order(order)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울시 강남구 역삼동 123")
                .deliveryRequest("문 앞에 놓아주세요")
                .build();
        deliveryRepository.save(delivery);
        delivery.ship("CJ대한통운", "kr.cjlogistics", "9876543210");
        delivery.deliver(); // deliveryStatus = DELIVERED
        deliveryRepository.save(delivery);

        // ── 6-5. Order 상태 DELIVERY_COMPLETED로 변경 ───────────────
        // completeDelivery(): orderStatus = DELIVERY_COMPLETED
        order.completeDelivery();
        orderRepository.save(order);

        log.info("[DataInitializer] 5-4 테스트 데이터 삽입 완료 — orderId={} 로 구매확정 테스트 가능", order.getOrderId());
    }
}
