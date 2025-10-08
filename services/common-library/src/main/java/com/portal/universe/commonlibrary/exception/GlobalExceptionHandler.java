package com.portal.universe.commonlibrary.exception;

import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomBusinessException.class)
    protected ResponseEntity<ApiResponse<Object>> handleCustomBusinessException(CustomBusinessException e) {
        log.error("handleCustomBusinessException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("handleException: {}", e.getMessage());
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }
}