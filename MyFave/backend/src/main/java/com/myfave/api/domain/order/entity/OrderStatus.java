package com.myfave.api.domain.order.entity;

public enum OrderStatus {
    PENDING,            // 결제 대기 중
    PAID,               // 결제 완료
    SHIPPING,           // 배송 중
    DELIVERY_COMPLETED, // 배송 완료
    PURCHASE_CONFIRMED, // 구매 확정
    CANCELLED,          // 주문 취소
    REFUNDED            // 환불 완료
}
