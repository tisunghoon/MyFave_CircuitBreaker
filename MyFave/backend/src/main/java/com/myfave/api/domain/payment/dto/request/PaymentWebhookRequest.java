package com.myfave.api.domain.payment.dto.request;

import lombok.Getter;

@Getter
public class PaymentWebhookRequest {

    private String type;
    private WebhookData data;

    @Getter
    public static class WebhookData {
        private String paymentId;      // PortOne pgTransactionId
        private String transactionId;
    }
}
