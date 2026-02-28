package com.portal.universe.paymentservice.intent.domain;

public enum PaymentIntentStatus {
    REQUIRES_PAYMENT,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    CANCELLED;

    public boolean isPayable() {
        return this == REQUIRES_PAYMENT || this == FAILED;
    }

    public boolean isCancellable() {
        return this == REQUIRES_PAYMENT || this == FAILED;
    }
}
