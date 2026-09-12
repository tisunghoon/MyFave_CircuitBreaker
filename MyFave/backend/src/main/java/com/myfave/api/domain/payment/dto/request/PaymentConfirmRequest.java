package com.myfave.api.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentConfirmRequest {

    @NotNull
    private Long paymentId;

    @NotBlank
    private String pgTransactionId;
}
