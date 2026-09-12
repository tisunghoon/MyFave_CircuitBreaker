package com.myfave.api.domain.order.service;

import com.myfave.api.domain.order.dto.response.OrderListResponse;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.entity.OrderItem;
import com.myfave.api.domain.order.entity.OrderType;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.product.entity.CategoryCode;
import com.myfave.api.domain.product.entity.ConditionCode;
import com.myfave.api.domain.product.entity.Product;
import com.myfave.api.domain.product.repository.ProductRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET /orders (OrderService.getOrders) 호출 시 발생하는 SQL 쿼리 수를 측정하는 통합 테스트.
 * 본 테스트는 findByOrderIn(N+1 방지 메서드)을 제거한 상태에서 실제로 N+1이 얼마나 발생하는지
 * 정량적으로 보여주기 위한 실측용 코드. application-test.yml의 generate_statistics 활성화 필요.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceN1Test {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    @BeforeEach
    void syncSequences() {
        // docker-compose의 init.sql이 미리 넣어둔 데이터로 인해 IDENTITY 시퀀스가
        // max(id)와 어긋난 상태이므로 매 테스트 시작 전 동기화 (트랜잭션 외부 동작)
        syncSeq("users", "user_id");
        syncSeq("products", "product_id");
        syncSeq("orders", "orders_id");
        syncSeq("order_items", "order_items_id");
    }

    private void syncSeq(String table, String col) {
        em.createNativeQuery(
                "SELECT setval(pg_get_serial_sequence('" + table + "', '" + col + "'), " +
                "GREATEST(COALESCE((SELECT MAX(" + col + ") FROM " + table + "), 0), 1))"
        ).getSingleResult();
    }

    @DisplayName("getOrders: 주문 N개 조회 시 발생 쿼리 수 측정 (findByOrderIn 제거 상태)")
    @ParameterizedTest(name = "주문 {0}개 → 쿼리 수 측정")
    @ValueSource(ints = {5, 10, 20, 50})
    void getOrders_queryCount_withoutBatchOptimization(int orderCount) {
        // given: 유저 1명, 상품 1개, 주문 orderCount개 + 각 주문당 OrderItem 2개
        User user = persistUser();
        Product product = persistProduct(user);
        for (int i = 0; i < orderCount; i++) {
            Order order = persistOrder(user, i);
            persistOrderItem(order, product);
            persistOrderItem(order, product);
        }
        em.flush();
        em.clear(); // 1차 캐시 비워서 실제 쿼리 발생 유도

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        // when
        long start = System.nanoTime();
        OrderListResponse response = orderService.getOrders(user.getUserId(), PageRequest.of(0, orderCount));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        long queryCount = stats.getPrepareStatementCount();

        // then
        System.out.printf("[N+1 측정] orders=%d → queries=%d, elapsed=%dms%n",
                orderCount, queryCount, elapsedMs);

        assertThat(response).isNotNull();
        // findByOrderIn 제거 → 1(User) + 1(Count) + 1(Order) + N(OrderItem) ≈ N+3 정도 기대
        // 회귀 보호: 최소 N개 이상의 쿼리가 발생해야 N+1 재현 확인
        assertThat(queryCount).isGreaterThanOrEqualTo(orderCount);
    }

    @DisplayName("getOrders: 주문 수가 늘어나면 쿼리 수도 선형 증가")
    @org.junit.jupiter.api.Test
    void getOrders_queryCount_scalesLinearly() {
        User user = persistUser();
        Product product = persistProduct(user);
        for (int i = 0; i < 30; i++) {
            Order order = persistOrder(user, i);
            persistOrderItem(order, product);
        }
        em.flush();
        em.clear();

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

        // 첫 번째 호출: 10건 조회
        stats.clear();
        em.clear();
        orderService.getOrders(user.getUserId(), PageRequest.of(0, 10));
        long count10 = stats.getPrepareStatementCount();

        // 두 번째 호출: 30건 조회
        stats.clear();
        em.clear();
        orderService.getOrders(user.getUserId(), PageRequest.of(0, 30));
        long count30 = stats.getPrepareStatementCount();

        System.out.printf("[선형성 검증] page=10 → queries=%d, page=30 → queries=%d%n", count10, count30);

        // 페이지 크기가 늘어나면 쿼리 수도 더 많아야 함 (단건 루프 특성)
        assertThat(count30).isGreaterThan(count10);
    }

    // ─── 픽스처 헬퍼 ─────────────────────────────────────────────────────────
    private static long seq = System.nanoTime();

    private User persistUser() {
        long s = seq++;
        // nickname: length=12 제약 → 최대 12자
        String nick = "n1" + (s % 1_000_000_000L); // 최대 11자 ("n1" + 9자리)
        User user = User.builder()
                .email("n1test-" + s + "@example.com")
                .password("password!")
                .name("N1Tester")
                .nickname(nick)
                .phone("010-" + String.format("%04d", s % 10000) + "-" + String.format("%04d", (s / 10000) % 10000))
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Product persistProduct(User user) {
        Product product = Product.builder()
                .user(user)
                .productName("테스트 상품")
                .price(10_000)
                .description("desc")
                .shortReview("리뷰")
                .size("M")
                .conditionCode(ConditionCode.A_GRADE)
                .categoryCode(CategoryCode.TOP)
                .build();
        return productRepository.saveAndFlush(product);
    }

    private Order persistOrder(User user, int idx) {
        long s = seq++;
        Order order = Order.builder()
                .user(user)
                .orderNumber("ORD-N1-" + s + "-" + idx)
                .orderType(OrderType.DIRECT)
                .build();
        return orderRepository.saveAndFlush(order);
    }

    private OrderItem persistOrderItem(Order order, Product product) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .price(product.getPrice())
                .productName(product.getProductName())
                .build();
        return orderItemRepository.saveAndFlush(item);
    }
}
