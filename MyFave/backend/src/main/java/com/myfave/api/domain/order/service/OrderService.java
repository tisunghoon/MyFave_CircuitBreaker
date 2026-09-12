package com.myfave.api.domain.order.service;

import com.myfave.api.domain.cart.entity.CartItem;
import com.myfave.api.domain.cart.repository.CartItemRepository;
import com.myfave.api.domain.order.dto.request.OrderCreateRequest;
import com.myfave.api.domain.order.dto.response.OrderConfirmResponse;
import com.myfave.api.domain.order.dto.response.OrderDetailResponse;
import com.myfave.api.domain.order.dto.response.OrderListResponse;
import com.myfave.api.domain.order.dto.response.OrderResponse;
import com.myfave.api.domain.order.dto.response.OrderSummaryResponse;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import com.myfave.api.domain.order.entity.OrderStatus;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.repository.ProductRepository;
import com.myfave.api.domain.shipping.entity.Delivery;
import com.myfave.api.domain.shipping.entity.ShippingAddress;
import com.myfave.api.domain.shipping.repository.DeliveryRepository;
import com.myfave.api.domain.shipping.repository.ShippingAddressRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용. 변경이 필요한 메서드에 별도 @Transactional 추가
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final DeliveryRepository deliveryRepository;
    private final MeterRegistry meterRegistry;

    // 주문 생성 (5-1)
    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {

        String type = request.getOrderType() != null ? request.getOrderType().name() : "UNKNOWN";
        String outcome = "failure";
        try {

        // ── 1. 사용자 조회 ──────────────────────────────────────────────
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        // JWT 토큰에서 꺼낸 userId로 User 엔티티를 DB에서 조회
        // 없으면 CustomException → GlobalExceptionHandler가 404 응답 반환
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // ── 2. 배송지 조회 ─────────────────────────────────────────────
        // 요청으로 받은 shippingAddressId가 실제로 DB에 존재하는지 확인
        ShippingAddress shippingAddress = shippingAddressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND));
        // 본인 소유 배송지인지 검증
        if (!shippingAddress.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.SHIPPING_ADDRESS_FORBIDDEN);
        }

        // ── 3. 주문 번호 생성 ──────────────────────────────────────────
        // 형식: ORD-yyyyMMdd-UUID앞8자리 (예: ORD-20260405-A1B2C3D4)
        // UUID는 매번 랜덤하게 생성되어 중복될 확률이 사실상 0에 가까움
        String orderNumber = "ORD-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // ── 5. Order 엔티티 생성 및 저장 ──────────────────────────────
        // Builder 패턴: 정해진 필드만 받아 Order 객체를 안전하게 생성
        // orderStatus는 Order.java 빌더 내부에서 자동으로 PENDING으로 세팅됨
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .orderType(request.getOrderType())
                .build();
        orderRepository.save(order); // INSERT INTO orders ... 실행

        // ── 6. orderType에 따라 재고 검증 + OrderItem 저장 ────────────
        // 차감 시점 정책: 결제 완료(completeConfirm)로 이동. Order 생성 단계는 validateStock만 수행.
        // 자세한 보상 흐름은 PaymentService.completeConfirm 참고.
        if (request.getOrderType() == OrderType.DIRECT) {
            // ── DIRECT: 단일 상품 바로 구매 ──────────────────────────
            if (request.getProductId() == null) {
                throw new CustomException(ErrorCode.ORDER_INVALID_ORDER_TYPE);
            }

            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            // 재고 검증만 수행 (실제 차감은 결제 승인 시점). 실패 시 PRODUCT_SOLD_OUT/STOCK_INSUFFICIENT.
            product.validateStock(1);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .price(product.getPrice())
                    .productName(product.getProductName())
                    .build();
            orderItemRepository.save(orderItem);

        } else {
            // ── CART: 장바구니 상품 구매 ─────────────────────────────
            if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
                throw new CustomException(ErrorCode.ORDER_INVALID_ORDER_TYPE);
            }

            // 정렬은 결정적인 처리 순서(로그 가독성·테스트 안정성)를 위해 유지. 락 미사용.
            List<Long> sortedProductIds = request.getProductIds().stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();

            List<Product> validatedProducts = new ArrayList<>();
            for (Long pid : sortedProductIds) {
                Product product = productRepository.findById(pid)
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
                product.validateStock(1);
                validatedProducts.add(product);
            }

            for (Product product : validatedProducts) {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .price(product.getPrice())
                        .productName(product.getProductName())
                        .build();
                orderItemRepository.save(orderItem);
            }
        }

        // ── 7. 응답 반환 ───────────────────────────────────────────────
        // OrderResponse.from(order): order 엔티티에서 필요한 필드만 뽑아 DTO로 변환
        int itemCount = orderItemRepository.findByOrder(order).size();
        log.info("[Order] 주문 생성: orderId={}, userId={}, type={}, itemCount={}",
                order.getOrderId(), userId, type, itemCount);
        outcome = "success";
        return OrderResponse.from(order);
        } finally {
            meterRegistry.counter("myfave.order.created",
                    "type", type, "outcome", outcome).increment();
        }
    }

    /**
     * 주문 목록 조회 (5-2)
     * 클래스 레벨 @Transactional(readOnly = true) 그대로 적용 (조회 전용)
     */
    public OrderListResponse getOrders(Long userId, Pageable pageable) {

        // ── 1. 사용자 조회 ──────────────────────────────────────────────
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // ── 2. 주문 목록 조회 (페이지네이션, 최신순) ────────────────────
        // Page<Order>: content(주문 목록) + totalElements, totalPages 등 메타 정보 포함
        Page<Order> orderPage = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        List<Order> orders = orderPage.getContent();

        // ── 3. 주문이 없으면 빈 응답 반환 ──────────────────────────────
        if (orders.isEmpty()) {
            return OrderListResponse.from(Page.empty(pageable));
        }

        // ── 4. OrderItem 주문별 단건 조회 (의도적 N+1 발생 — 부하 실측용) ─
        // 운영에서는 findByOrderIn(orders)로 IN 쿼리 1회 사용. 실측을 위해 단건 루프로 교체.
        Map<Long, List<OrderItem>> itemsByOrderId = orders.stream()
                .collect(Collectors.toMap(
                        Order::getOrderId,
                        order -> orderItemRepository.findByOrder(order)
                ));

        // ── 6. 주문별 DTO 변환 ──────────────────────────────────────────
        List<OrderSummaryResponse> summaries = orders.stream()
                .map(order -> OrderSummaryResponse.from(
                        order,
                        // 해당 주문의 OrderItem 목록 (없으면 빈 리스트)
                        itemsByOrderId.getOrDefault(order.getOrderId(), List.of())
                ))
                .toList();

        // ── 7. Page<OrderSummaryResponse>로 래핑 후 반환 ────────────────
        // PageImpl: content + pageable + totalElements를 조합해 Page 객체 생성
        Page<OrderSummaryResponse> summaryPage =
                new PageImpl<>(summaries, pageable, orderPage.getTotalElements());
        return OrderListResponse.from(summaryPage);
    }

    /**
     * 주문 상세 조회 (5-3)
     * 클래스 레벨 @Transactional(readOnly = true) 그대로 적용 (조회 전용)
     */
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {

        // ── 1. userId null 체크 ──────────────────────────────────────────
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // ── 2. orderId → Order 조회 ──────────────────────────────────────
        // 없으면 CustomException → GlobalExceptionHandler가 404 응답 반환
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // ── 3. 본인 주문 확인 ────────────────────────────────────────────
        // 로그인한 사람(userId)과 주문자(order.getUser())가 같아야 함
        if (!order.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // ── 4. OrderItem 조회 ────────────────────────────────────────────
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        // ── 5. Delivery 조회 (배송 생성 전이면 null) ──────────────────────
        Delivery delivery = deliveryRepository.findByOrder(order).orElse(null);

        // ── 6. Payment 조회 (미결제 상태이면 null) ───────────────────────
        // Order.finalPayment: completePay() 호출 시 세팅, 미결제이면 null
        Payment payment = order.getFinalPayment();

        // ── 7. OrderDetailResponse 반환 ───────────────────────────────────
        return OrderDetailResponse.from(order, payment, delivery, orderItems);
    }

    /**
     * 주문 상태 변경 - 구매확정 (5-4)
     * @Transactional: orderStatus를 PURCHASE_CONFIRMED로 변경하므로 쓰기 트랜잭션 적용
     */
    @Transactional
    public OrderConfirmResponse confirmOrder(Long userId, Long orderId) {

        // ── 1. userId null 체크 ──────────────────────────────────────────
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // ── 2. orderId → Order 조회 ──────────────────────────────────────
        // 없으면 CustomException → GlobalExceptionHandler가 404 응답 반환
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // ── 3. 본인 주문 확인 ────────────────────────────────────────────
        // 로그인한 사람(userId)과 주문자(order.getUser())가 같아야 함
        if (!order.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // ── 4. 구매확정 가능 상태 확인 ───────────────────────────────────
        // 스펙: DELIVERY_COMPLETED 상태인 주문만 구매확정 가능
        // 그 외 상태(PENDING, PAID, SHIPPING 등)이면 409 반환
        if (order.getOrderStatus() != OrderStatus.DELIVERY_COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATUS);
        }

        // ── 5. 구매확정 처리 ─────────────────────────────────────────────
        // order.confirm(): orderStatus를 PURCHASE_CONFIRMED로 변경
        // @Transactional이므로 메서드 종료 시 JPA가 변경 감지 → UPDATE 쿼리 자동 실행
        order.confirm();
        meterRegistry.counter("myfave.order.status.transition",
                "from", "DELIVERY_COMPLETED", "to", "PURCHASE_CONFIRMED").increment();
        log.info("[Order] 구매확정: orderId={}, userId={}", orderId, userId);

        // ── 6. 응답 반환 ─────────────────────────────────────────────────
        // OrderConfirmResponse.from(order): orderId, orderStatus(PURCHASE_CONFIRMED) 반환
        return OrderConfirmResponse.from(order);
    }

    /**
     * 주문 취소 (PENDING 상태에 한해 허용) — 단순 상태 전환
     * - 차감 시점 정책이 "결제 완료(completeConfirm) 시점"으로 이동했기 때문에
     *   PENDING 주문은 재고를 차지하지 않는다 → 재고 복구 로직 불필요.
     * - PAID 이후 단계의 취소·환불은 PaymentService.cancelPayment 경로를 사용한다.
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {

        // ── 1. userId null 체크 ──────────────────────────────────────────
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // ── 2. orderId → Order 조회 ──────────────────────────────────────
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // ── 3. 본인 주문 확인 ────────────────────────────────────────────
        if (!order.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // ── 4. 취소 가능 상태 확인 ───────────────────────────────────────
        OrderStatus status = order.getOrderStatus();
        if (status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if (status != OrderStatus.PENDING) {
            // PAID 이후 단계는 PaymentService.cancelPayment 경로로 유도
            throw new CustomException(ErrorCode.ORDER_CANCEL_FORBIDDEN);
        }

        // ── 5. 주문 상태 CANCELLED 전환 ─────────────────────────────────
        // 재고 복구 로직 없음 — Order 생성 시점에 차감하지 않는 정책
        order.cancel();
        meterRegistry.counter("myfave.order.status.transition",
                "from", status.name(), "to", "CANCELLED").increment();
        log.info("[Order] 주문 취소: orderId={}, userId={}, fromStatus={}", orderId, userId, status);
    }
}
