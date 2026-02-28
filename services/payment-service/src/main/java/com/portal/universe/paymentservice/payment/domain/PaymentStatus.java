package com.portal.universe.paymentservice.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED;

    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }

    public boolean isRefundable() {
        return this == COMPLETED;
    }
}
