package com.myfave.api.domain.payment.dto.response;

import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.entity.PaymentMethod;
import com.myfave.api.domain.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private PaymentMethod paymentMethod;
    private Integer totalProductPrice;
    private Integer deliveryFee;
    private Integer discountPrice;
    private Integer totalPaymentPrice;
    private Integer refundedAmount;
    private PaymentStatus paymentStatus;
    private String pgTransactionId;
    private String receiptUrl;
    private ZonedDateTime paidAt;
    private String failReason;

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getPaymentMethod(),
                payment.getTotalProductPrice(),
                payment.getDeliveryFee(),
                payment.getDiscountPrice(),
                payment.getTotalPaymentPrice(),
                payment.getRefundedAmount(),
                payment.getPaymentStatus(),
                payment.getPgTransactionId(),
                payment.getReceiptUrl(),
                payment.getPaidAt(),
                payment.getFailReason()
        );
    }
}
