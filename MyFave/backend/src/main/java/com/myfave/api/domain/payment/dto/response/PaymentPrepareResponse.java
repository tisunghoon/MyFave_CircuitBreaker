package com.myfave.api.domain.payment.dto.response;

import com.myfave.api.domain.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentPrepareResponse {

    private Long paymentId;
    private String idempotencyKey;
    private String storeId;
    private String channelKey;
    private Integer totalProductPrice;
    private Integer deliveryFee;
    private Integer discountPrice;
    private Integer totalPaymentPrice;

    public static PaymentPrepareResponse of(Payment payment, String storeId, String channelKey) {
        return new PaymentPrepareResponse(
                payment.getPaymentId(),
                payment.getIdempotencyKey(),
                storeId,
                channelKey,
                payment.getTotalProductPrice(),
                payment.getDeliveryFee(),
                payment.getDiscountPrice(),
                payment.getTotalPaymentPrice()
        );
    }
}