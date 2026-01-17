package com.portal.universe.shoppingservice.payment.pg;

/**
 * PG사 응답 DTO입니다.
 */
public record PgResponse(
        boolean success,
        String transactionId,
        String message,
        String errorCode,
        String rawResponse
) {
    public static PgResponse success(String transactionId) {
        return new PgResponse(
                true,
                transactionId,
                "Payment processed successfully",
                null,
                "{\"status\":\"SUCCESS\",\"txId\":\"" + transactionId + "\"}"
        );
    }

    public static PgResponse failure(String errorCode, String message) {
        return new PgResponse(
                false,
                null,
                message,
                errorCode,
                "{\"status\":\"FAILED\",\"errorCode\":\"" + errorCode + "\",\"message\":\"" + message + "\"}"
        );
    }
}
