package com.portal.universe.paymentservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    // Intent errors (PM01x)
    INTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PM010", "Payment intent not found"),
    INTENT_NOT_PAYABLE(HttpStatus.CONFLICT, "PM011", "Payment intent is not in payable state"),
    INTENT_EXPIRED(HttpStatus.GONE, "PM012", "Payment intent has expired"),
    INTENT_USER_MISMATCH(HttpStatus.FORBIDDEN, "PM013", "Payment intent does not belong to this user"),
    INTENT_CANNOT_BE_CANCELLED(HttpStatus.CONFLICT, "PM014", "Payment intent cannot be cancelled"),
    INTENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PM015", "Payment intent already exists for this order"),

    // Payment errors (PM02x)
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PM020", "Payment not found"),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "PM021", "Payment has already been completed"),
    PAYMENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PM022", "Payment processing failed"),
    PAYMENT_CANNOT_BE_CANCELLED(HttpStatus.CONFLICT, "PM023", "Payment cannot be cancelled"),
    PAYMENT_REFUND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PM024", "Payment refund failed"),
    PAYMENT_USER_MISMATCH(HttpStatus.FORBIDDEN, "PM025", "Payment does not belong to this user"),

    // Internal errors (PM09x)
    INTERNAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "PM090", "Invalid internal service token");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
