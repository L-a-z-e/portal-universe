package com.portal.universe.paymentservice.payment.pg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class MockPGClient {

    private static final double SUCCESS_RATE = 0.9;

    public PgResponse processPayment(String paymentNumber, BigDecimal amount,
                                     String paymentMethod, String cardNumber) {
        log.info("Processing payment: {} (amount: {}, method: {})", paymentNumber, amount, paymentMethod);

        boolean success = ThreadLocalRandom.current().nextDouble() < SUCCESS_RATE;

        if (success) {
            String txnId = "PG-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            return new PgResponse(true, txnId, null, "Payment approved",
                    "{\"status\":\"approved\",\"txnId\":\"" + txnId + "\"}");
        } else {
            return new PgResponse(false, null, "PG_DECLINED", "Insufficient funds",
                    "{\"status\":\"declined\",\"reason\":\"insufficient_funds\"}");
        }
    }

    public PgResponse refundPayment(String pgTransactionId, BigDecimal amount) {
        log.info("Refunding payment: {} (amount: {})", pgTransactionId, amount);
        String txnId = "RF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return new PgResponse(true, txnId, null, "Refund approved",
                "{\"status\":\"refunded\",\"txnId\":\"" + txnId + "\"}");
    }
}
