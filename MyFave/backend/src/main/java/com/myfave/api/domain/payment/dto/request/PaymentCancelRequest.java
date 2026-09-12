package com.myfave.api.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PaymentCancelRequest {

    @NotBlank
    private String reason;

    private Integer refundAmount; // null = 전체 취소, 값 있으면 부분 취소
}
