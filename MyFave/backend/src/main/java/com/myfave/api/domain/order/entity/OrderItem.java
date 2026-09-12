package com.myfave.api.domain.order.entity;

import com.myfave.api.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Entity
@Table(name = "order_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_items_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Integer price;          // 주문 당시 가격 스냅샷

    @Column(nullable = false, length = 100)
    private String productName;     // 주문 당시 상품명 스냅샷

    @CreatedDate
    @Column(updatable = false)
    private ZonedDateTime createdAt;

    @Builder
    private OrderItem(Product product, Order order, Integer price, String productName) {
        this.product = product;
        this.order = order;
        this.price = price;
        this.productName = productName;
    }
}
