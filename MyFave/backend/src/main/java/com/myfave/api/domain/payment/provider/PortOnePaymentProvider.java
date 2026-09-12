package com.myfave.api.domain.payment.provider;

import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class PortOnePaymentProvider implements PaymentProvider {

    private final WebClient webClient;
    private final MeterRegistry meterRegistry;

    public PortOnePaymentProvider(
            WebClient.Builder builder,
            @Value("${portone.api-url}") String apiUrl,
            @Value("${portone.api-secret}") String apiSecret,
            MeterRegistry meterRegistry
    ) {
        this.webClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "PortOne " + apiSecret)
                .build();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public PortOnePaymentInfo getPaymentInfo(String pgTransactionId) {
        return invokeWithMetrics("getPaymentInfo", () -> {
            log.debug("[PortOne] getPaymentInfo 호출: pgTxId={}", pgTransactionId);
            Map<?, ?> body = webClient.get()
                    .uri("/payments/{id}", pgTransactionId)
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        int status = response.statusCode().value();
                        recordHttpError("getPaymentInfo", status);
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(err -> {
                                    log.warn("[PortOne] getPaymentInfo HTTP 에러: pgTxId={}, status={}, body={}",
                                            pgTransactionId, status, err);
                                    return new CustomException(ErrorCode.PAYMENT_FAILED);
                                });
                    })
                    .bodyToMono(Map.class)
                    .block();

            if (body == null) {
                log.warn("[PortOne] getPaymentInfo 응답 본문 비어있음: pgTxId={}", pgTransactionId);
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }

            String status = (String) body.get("status");
            int totalAmount = ((Number) ((Map<?, ?>) body.get("amount")).get("total")).intValue();
            String receiptUrl = (String) body.get("receiptUrl");
            ZonedDateTime paidAt = body.get("paidAt") != null
                    ? ZonedDateTime.parse((String) body.get("paidAt"))
                    : null;

            return new PortOnePaymentInfo(pgTransactionId, status, totalAmount, receiptUrl, paidAt);
        });
    }

    @Override
    public void cancelPayment(String pgTransactionId, int cancelAmount, String reason) {
        invokeWithMetrics("cancelPayment", () -> {
            log.debug("[PortOne] cancelPayment 호출: pgTxId={}, amount={}, reason={}",
                    pgTransactionId, cancelAmount, reason);
            Map<?, ?> body = webClient.post()
                    .uri("/payments/{id}/cancel", pgTransactionId)
                    .bodyValue(Map.of(
                            "reason", reason,
                            "amount", cancelAmount
                    ))
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        int status = response.statusCode().value();
                        recordHttpError("cancelPayment", status);
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(err -> {
                                    log.warn("[PortOne] cancelPayment HTTP 에러: pgTxId={}, status={}, body={}",
                                            pgTransactionId, status, err);
                                    return new CustomException(ErrorCode.PAYMENT_FAILED);
                                });
                    })
                    .bodyToMono(Map.class)
                    .block();

            if (body == null) {
                log.warn("[PortOne] cancelPayment 응답 본문 비어있음: pgTxId={}", pgTransactionId);
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }
            return null;
        });
    }

    // PG 호출을 Timer.Sample로 감싸 outcome(success/failure)별 latency 분포 측정.
    // 서드파티 SLA와 우리 서비스 latency를 분리해서 보기 위함.
    private <T> T invokeWithMetrics(String api, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        try {
            T result = call.get();
            outcome = "success";
            return result;
        } finally {
            sample.stop(Timer.builder("myfave.portone.call.duration")
                    .tag("api", api)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private void recordHttpError(String api, int statusCode) {
        String statusClass = statusCode >= 500 ? "5xx" : statusCode >= 400 ? "4xx" : "other";
        meterRegistry.counter("myfave.portone.http.errors",
                "api", api, "status_class", statusClass).increment();
    }
}
