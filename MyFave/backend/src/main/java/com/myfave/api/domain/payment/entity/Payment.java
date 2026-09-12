package com.myfave.api.domain.payment.entity;

import com.myfave.api.domain.coupon.entity.Coupon;
import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_coupon_id")
    private Coupon discountCoupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_coupon_id")
    private Coupon shippingCoupon;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "pg_provider", nullable = false, length = 50)
    private String pgProvider;

    @Column(name = "pg_transaction_id", length = 255)
    private String pgTransactionId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private Integer totalProductPrice = 0;

    @Column(nullable = false)
    private Integer deliveryFee = 0;

    @Column(nullable = false)
    private Integer discountPrice = 0;

    @Column(nullable = false)
    private Integer totalPaymentPrice = 0;

    @Column(nullable = false)
    private Integer refundedAmount = 0;

    @Column(nullable = false)
    private Integer pgFee = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String failReason;

    @Column(columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @Builder
    private Payment(Order order, Coupon discountCoupon, Coupon shippingCoupon,
                    String idempotencyKey, String pgProvider,
                    PaymentMethod paymentMethod,
                    Integer totalProductPrice, Integer deliveryFee,
                    Integer discountPrice, Integer totalPaymentPrice) {
        this.order = order;
        this.discountCoupon = discountCoupon;
        this.shippingCoupon = shippingCoupon;
        this.idempotencyKey = idempotencyKey;
        this.pgProvider = pgProvider;
        this.paymentMethod = paymentMethod;
        this.totalProductPrice = totalProductPrice != null ? totalProductPrice : 0;
        this.deliveryFee = deliveryFee != null ? deliveryFee : 0;
        this.discountPrice = discountPrice != null ? discountPrice : 0;
        this.totalPaymentPrice = totalPaymentPrice != null ? totalPaymentPrice : 0;
        this.refundedAmount = 0;
        this.pgFee = 0;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    // PENDING → AUTHORIZED
    public void authorize(String pgTransactionId) {
        this.pgTransactionId = pgTransactionId;
        this.paymentStatus = PaymentStatus.AUTHORIZED;
    }

    // AUTHORIZED → COMPLETED
    public void complete(String receiptUrl, ZonedDateTime paidAt) {
        this.receiptUrl = receiptUrl;
        this.paidAt = paidAt;
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    // → FAILED
    public void fail(String failReason) {
        this.failReason = failReason;
        this.paymentStatus = PaymentStatus.FAILED;
    }

    // PG 트랜잭션 ID 만 기록 (status 전이 없이) — 실패 분기에서 웹훅 복구 키 보존용.
    // 이미 값이 있으면 덮어쓰지 않음 (멱등성). null/blank 입력은 무시 (CR PR#185 C1).
    public void recordPgTransactionId(String pgTransactionId) {
        if (pgTransactionId == null || pgTransactionId.isBlank()) return;
        if (this.pgTransactionId == null) {
            this.pgTransactionId = pgTransactionId;
        }
    }

    // COMPLETED → CANCELLED
    public void cancel() {
        this.paymentStatus = PaymentStatus.CANCELLED;
    }

    // COMPLETED → PARTIAL_CANCELLED
    public void partialCancel(int refundAmount) {
        this.refundedAmount += refundAmount;
        this.paymentStatus = PaymentStatus.PARTIAL_CANCELLED;
    }
}