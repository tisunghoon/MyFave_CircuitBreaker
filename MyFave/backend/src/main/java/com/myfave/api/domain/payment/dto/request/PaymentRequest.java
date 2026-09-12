package com.myfave.api.domain.payment.dto.request;

import com.myfave.api.domain.payment.entity.PaymentMethod;
import lombok.Getter;

@Getter
public class PaymentRequest {

    private Long orderId;
    private PaymentMethod paymentMethod;
    private Long couponId;      // null = 쿠폰 미사용
    private Integer totalPaymentPrice;
}
