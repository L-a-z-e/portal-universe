package com.portal.universe.shoppingservice.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 쇼핑(Shopping) 서비스에서 발생하는 비즈니스 예외에 대한 오류 코드를 정의하는 열거형 클래스입니다.
 */
@Getter
public enum ShoppingErrorCode implements ErrorCode {

    /**
     * 요청한 ID에 해당하는 상품이 존재하지 않을 경우 발생합니다.
     */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ShoppingErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}