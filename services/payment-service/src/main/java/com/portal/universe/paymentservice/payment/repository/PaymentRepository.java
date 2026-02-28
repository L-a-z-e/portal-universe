package com.portal.universe.paymentservice.payment.repository;

import com.portal.universe.paymentservice.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    Optional<Payment> findByOrderNumber(String orderNumber);

    Optional<Payment> findByIntentId(String intentId);
}
