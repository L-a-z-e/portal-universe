package com.portal.universe.paymentservice.intent.repository;

import com.portal.universe.paymentservice.intent.domain.PaymentIntent;
import com.portal.universe.paymentservice.intent.domain.PaymentIntentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

    Optional<PaymentIntent> findByIntentId(String intentId);

    Optional<PaymentIntent> findByOrderNumber(String orderNumber);

    @Query("""
            SELECT pi FROM PaymentIntent pi
            WHERE pi.status = :status AND pi.expiresAt < :now
            """)
    List<PaymentIntent> findExpiredIntents(
            @Param("status") PaymentIntentStatus status,
            @Param("now") Instant now);
}
