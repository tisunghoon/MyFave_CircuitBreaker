package com.myfave.api.domain.payment.entity;

public enum PaymentStatus {
    PENDING,           // DB 저장 완료, PG 호출 전
    AUTHORIZED,        // PG 승인 성공 (금액 검증 전)
    COMPLETED,         // 금액 검증까지 통과, 주문 확정
    FAILED,            // PG 승인 실패 또는 금액 불일치
    CANCELLED,         // 전액 취소
    PARTIAL_CANCELLED  // 부분 취소
}
