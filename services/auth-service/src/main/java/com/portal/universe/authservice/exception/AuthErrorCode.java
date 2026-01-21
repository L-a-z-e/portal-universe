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
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "Email already exists"),

    /**
     * 로그인 실패 - 이메일 또는 비밀번호가 일치하지 않습니다.
     */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "Invalid email or password"),

    /**
     * Refresh Token 검증 실패 - 토큰이 유효하지 않거나 만료되었습니다.
     */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Invalid or expired refresh token"),

    /**
     * 사용자를 찾을 수 없습니다.
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A004", "User not found"),

    /**
     * 유효하지 않은 토큰입니다.
     */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "Invalid token"),

    /**
     * 소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.
     */
    SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "A006", "Social login users cannot change password"),

    /**
     * 현재 비밀번호가 일치하지 않습니다.
     */
    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "A007", "Current password is incorrect"),

    /**
     * 비밀번호 확인이 일치하지 않습니다.
     */
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "A008", "Password confirmation does not match"),

    /**
     * 회원 탈퇴 시 비밀번호가 일치하지 않습니다.
     */
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A009", "Password is incorrect"),

    /**
     * Username이 이미 존재합니다.
     */
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A011", "Username already exists"),

    /**
     * Username이 이미 설정되어 변경할 수 없습니다.
     */
    USERNAME_ALREADY_SET(HttpStatus.BAD_REQUEST, "A012", "Username already set"),

    /**
     * Username 형식이 올바르지 않습니다.
     */
    INVALID_USERNAME_FORMAT(HttpStatus.BAD_REQUEST, "A013", "Invalid username format. Only lowercase letters, numbers, and underscores are allowed (3-20 characters)");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}