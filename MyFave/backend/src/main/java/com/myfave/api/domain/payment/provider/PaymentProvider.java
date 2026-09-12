package com.myfave.api.domain.payment.provider;

import java.time.ZonedDateTime;

public interface PaymentProvider {

    PortOnePaymentInfo getPaymentInfo(String pgTransactionId);

    void cancelPayment(String pgTransactionId, int cancelAmount, String reason);

    record PortOnePaymentInfo(
            String pgTransactionId,
            String status,
            int totalAmount,
            String receiptUrl,
            ZonedDateTime paidAt
    ) {}
}