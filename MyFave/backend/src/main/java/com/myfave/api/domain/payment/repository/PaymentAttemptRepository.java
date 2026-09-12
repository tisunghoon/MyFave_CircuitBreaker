package com.myfave.api.domain.payment.repository;

import com.myfave.api.domain.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByPaymentPaymentIdOrderByAttemptNoAsc(Long paymentId);

    int countByPaymentPaymentId(Long paymentId);
}