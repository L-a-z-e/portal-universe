package com.portal.universe.commonlibrary.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 여러 서비스에서 공통적으로 발생할 수 있는 기본 오류 코드를 정의하는 열거형 클래스입니다.
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    /**
     * 서버 내부의 예상치 못한 오류가 발생했을 경우 사용됩니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "Internal Server Error"),

    /**
     * 클라이언트의 요청 값이 유효하지 않을 경우(예: 형식 불일치, 값 누락) 사용됩니다.
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "Invalid Input Value"),

    /**
     * 요청한 리소스를 찾을 수 없을 경우 사용됩니다.
     */
    NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "Not Found"),

    /**
     * 권한이 없거나 허용되지 않는 상태에서 작업이 수행될 경우 사용됩니다.
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "C004", "Forbidden"),

    /**
     * 인증되지 않은 사용자가 보호된 리소스에 접근할 경우 사용됩니다.
     * JWT 토큰이 없거나, 만료되었거나, 유효하지 않은 경우에 해당합니다.
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C005", "Unauthorized");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommonErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}