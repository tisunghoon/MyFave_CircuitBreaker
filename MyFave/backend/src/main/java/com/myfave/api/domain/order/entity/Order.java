package com.myfave.api.domain.order.entity;

import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orders_id")
    private Long orderId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    // 순환 FK: payments.orders_id ↔ orders.final_payment_id (DEFERRABLE INITIALLY DEFERRED)
    // 결제 성공 후 서비스 레이어에서 UPDATE로 세팅
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_payment_id")
    private Payment finalPayment;

    @Builder
    private Order(User user, String orderNumber, OrderType orderType) {
        this.user = user;
        this.orderNumber = orderNumber;
        this.orderType = orderType;
        this.orderStatus = OrderStatus.PENDING;
    }

    public void completePay(Payment payment) {
        this.finalPayment = payment;
        this.orderStatus = OrderStatus.PAID;
    }

    public void cancel() {
        this.orderStatus = OrderStatus.CANCELLED;
    }

    public void completeDelivery() {
        this.orderStatus = OrderStatus.DELIVERY_COMPLETED;
    }

    public void confirm() {
        this.orderStatus = OrderStatus.PURCHASE_CONFIRMED;
    }

    public void refund() {
        this.orderStatus = OrderStatus.REFUNDED;
    }
}


