package com.myfave.api.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Integer attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus attemptStatus;

    @Column(name = "pg_transaction_id", length = 255)
    private String pgTransactionId;

    @Column(columnDefinition = "TEXT")
    private String pgResponse;

    @Column(columnDefinition = "TEXT")
    private String failReason;

    @Column(nullable = false, updatable = false)
    private ZonedDateTime attemptedAt;

    @Builder
    private PaymentAttempt(Payment payment, Integer attemptNo, PaymentStatus attemptStatus,
                           String pgTransactionId, String pgResponse, String failReason) {
        this.payment = payment;
        this.attemptNo = attemptNo;
        this.attemptStatus = attemptStatus;
        this.pgTransactionId = pgTransactionId;
        this.pgResponse = pgResponse;
        this.failReason = failReason;
        this.attemptedAt = ZonedDateTime.now();
    }
}