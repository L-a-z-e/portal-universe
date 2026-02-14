package com.portal.universe.shoppingsettlementservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SettlementErrorCode implements ErrorCode {

    PERIOD_NOT_FOUND(HttpStatus.NOT_FOUND, "ST001", "Settlement period not found"),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ST002", "Settlement not found"),
    PERIOD_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "ST003", "Settlement period already completed"),
    PERIOD_NOT_CALCULATED(HttpStatus.BAD_REQUEST, "ST004", "Settlement period has not been calculated yet"),
    BATCH_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ST005", "Batch job execution failed"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ST006", "Invalid date range"),
    SETTLEMENT_ALREADY_PAID(HttpStatus.BAD_REQUEST, "ST007", "Settlement has already been paid");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SettlementErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
