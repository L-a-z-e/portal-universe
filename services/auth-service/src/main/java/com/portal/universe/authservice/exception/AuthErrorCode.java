package com.portal.universe.authservice.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증(Auth) 서비스에서 발생하는 비즈니스 예외에 대한 오류 코드를 정의하는 열거형 클래스입니다.
 * 각 오류 코드는 HTTP 상태, 고유 코드, 그리고 메시지를 포함합니다.
 */
@Getter
public enum AuthErrorCode implements ErrorCode {

    /**
     * 사용자가 회원가입 시도 시, 해당 이메일이 이미 데이터베이스에 존재할 경우 발생합니다.
     */
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "Email already exists");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}