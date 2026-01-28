package com.portal.universe.authservice.common.exception;

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
    INVALID_USERNAME_FORMAT(HttpStatus.BAD_REQUEST, "A013", "Invalid username format. Only lowercase letters, numbers, and underscores are allowed (3-20 characters)"),

    /**
     * 이미 팔로우 중인 사용자입니다.
     */
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "A014", "Already following this user"),

    /**
     * 팔로우하지 않은 사용자입니다.
     */
    NOT_FOLLOWING(HttpStatus.NOT_FOUND, "A015", "Not following this user"),

    /**
     * 자기 자신을 팔로우할 수 없습니다.
     */
    CANNOT_FOLLOW_YOURSELF(HttpStatus.BAD_REQUEST, "A016", "Cannot follow yourself"),

    /**
     * 팔로우 대상 사용자를 찾을 수 없습니다.
     */
    FOLLOW_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A017", "Target user not found"),

    /**
     * 계정이 일시적으로 잠겼습니다.
     */
    ACCOUNT_TEMPORARILY_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "A018", "Account temporarily locked. Please try again after {0} minute(s)"),

    /**
     * 로그인 시도 횟수가 초과되었습니다.
     */
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "A019", "Too many login attempts"),

    /**
     * 비밀번호 길이가 최소 요구사항을 만족하지 않습니다.
     */
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "A020", "Password must be at least {0} characters long"),

    /**
     * 비밀번호가 정책 요구사항을 만족하지 않습니다.
     */
    PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "A021", "Password must contain uppercase, lowercase, digit, and special character"),

    /**
     * 최근 사용한 비밀번호는 재사용할 수 없습니다.
     */
    PASSWORD_RECENTLY_USED(HttpStatus.BAD_REQUEST, "A022", "Cannot reuse recently used passwords"),

    /**
     * 비밀번호에 사용자 정보를 포함할 수 없습니다.
     */
    PASSWORD_CONTAINS_USER_INFO(HttpStatus.BAD_REQUEST, "A023", "Password cannot contain user information"),

    /**
     * 비밀번호가 만료되었습니다.
     */
    PASSWORD_EXPIRED(HttpStatus.UNAUTHORIZED, "A024", "Password has expired. Please set a new password"),

    /**
     * 비밀번호 길이가 최대 허용 길이를 초과했습니다.
     */
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "A025", "Password must not exceed {0} characters"),

    /**
     * 비밀번호에 연속된 문자를 사용할 수 없습니다.
     */
    PASSWORD_CONTAINS_SEQUENTIAL(HttpStatus.BAD_REQUEST, "A026", "Password cannot contain sequential characters"),

    // RBAC
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "A030", "Role not found"),
    ROLE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "A031", "Role already assigned to user"),
    ROLE_NOT_ASSIGNED(HttpStatus.NOT_FOUND, "A032", "Role not assigned to user"),
    SYSTEM_ROLE_CANNOT_BE_MODIFIED(HttpStatus.BAD_REQUEST, "A033", "System role cannot be modified or deleted"),
    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "A034", "Permission not found"),

    // Membership
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "A035", "Membership not found"),
    MEMBERSHIP_TIER_NOT_FOUND(HttpStatus.NOT_FOUND, "A036", "Membership tier not found"),
    MEMBERSHIP_ALREADY_EXISTS(HttpStatus.CONFLICT, "A037", "Membership already exists for this service"),
    MEMBERSHIP_EXPIRED(HttpStatus.FORBIDDEN, "A038", "Membership has expired"),

    // Seller
    SELLER_APPLICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "A040", "Seller application already pending"),
    SELLER_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "A041", "Seller application not found"),
    SELLER_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "A042", "Seller application has already been processed");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
