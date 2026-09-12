package com.myfave.api.domain.payment.provider;

import com.myfave.api.domain.payment.entity.Payment;
import com.myfave.api.domain.payment.repository.PaymentRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

// 부하 테스트 전용 PaymentProvider mock.
// 외부 PortOne API 호출 없이 항상 PAID 상태로 응답해 시나리오 D의 PESSIMISTIC 락 검증을 가능하게 함.
// pgTransactionId 패턴 "MOCK-PAY-{paymentId}"에서 paymentId 파싱 → DB에서 실제 결제 금액 조회.
@Slf4j
@Component
@Profile("loadtest")
@Primary
@RequiredArgsConstructor
public class MockPortOnePaymentProvider implements PaymentProvider {

    private static final String MOCK_PREFIX = "MOCK-PAY-";

    private final PaymentRepository paymentRepository;

    @Override
    public PortOnePaymentInfo getPaymentInfo(String pgTransactionId) {
        int amount = resolveAmount(pgTransactionId);
        return new PortOnePaymentInfo(
                pgTransactionId,
                "PAID",
                amount,
                "https://mock.receipt/" + pgTransactionId,
                ZonedDateTime.now()
        );
    }

    @Override
    public void cancelPayment(String pgTransactionId, int cancelAmount, String reason) {
        log.debug("[Mock] cancel pgTransactionId={}, amount={}, reason={}",
                pgTransactionId, cancelAmount, reason);
    }

    private int resolveAmount(String pgTransactionId) {
        if (pgTransactionId == null || !pgTransactionId.startsWith(MOCK_PREFIX)) {
            return 13000;
        }
        try {
            Long paymentId = Long.parseLong(pgTransactionId.substring(MOCK_PREFIX.length()));
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
            return payment.getTotalPaymentPrice();
        } catch (NumberFormatException e) {
            return 13000;
        }
    }
}
