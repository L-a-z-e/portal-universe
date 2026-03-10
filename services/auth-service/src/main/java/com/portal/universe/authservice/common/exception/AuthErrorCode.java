package com.portal.universe.authservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Auth 서비스 비즈니스 예외 코드.
 */
@Getter
public enum AuthErrorCode implements ErrorCode {

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "Email already exists"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "Invalid email or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Invalid or expired refresh token"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A004", "User not found"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "Invalid token"),
    SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "A006", "Social login users cannot change password"),
    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "A007", "Current password is incorrect"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "A008", "Password confirmation does not match"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A009", "Password is incorrect"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "A010", "Account is not active"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A011", "Username already exists"),
    USERNAME_ALREADY_SET(HttpStatus.BAD_REQUEST, "A012", "Username already set"),
    INVALID_USERNAME_FORMAT(HttpStatus.BAD_REQUEST, "A013", "Invalid username format. Only lowercase letters, numbers, and underscores are allowed (3-20 characters)"),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "A014", "Already following this user"),
    NOT_FOLLOWING(HttpStatus.NOT_FOUND, "A015", "Not following this user"),
    CANNOT_FOLLOW_YOURSELF(HttpStatus.BAD_REQUEST, "A016", "Cannot follow yourself"),
    FOLLOW_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A017", "Target user not found"),
    ACCOUNT_TEMPORARILY_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "A018", "Account temporarily locked. Please try again after {0} minute(s)"),
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "A019", "Too many login attempts"),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "A020", "Password must be at least {0} characters long"),
    PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "A021", "Password must contain uppercase, lowercase, digit, and special character"),
    PASSWORD_RECENTLY_USED(HttpStatus.BAD_REQUEST, "A022", "Cannot reuse recently used passwords"),
    PASSWORD_CONTAINS_USER_INFO(HttpStatus.BAD_REQUEST, "A023", "Password cannot contain user information"),
    PASSWORD_EXPIRED(HttpStatus.UNAUTHORIZED, "A024", "Password has expired. Please set a new password"),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "A025", "Password must not exceed {0} characters"),
    PASSWORD_CONTAINS_SEQUENTIAL(HttpStatus.BAD_REQUEST, "A026", "Password cannot contain sequential characters"),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "A027", "Invalid or expired password reset token"),
    PASSWORD_RESET_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "A028", "Too many password reset requests. Please try again later"),

    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "A030", "Role not found"),
    ROLE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "A031", "Role already assigned to user"),
    ROLE_NOT_ASSIGNED(HttpStatus.NOT_FOUND, "A032", "Role not assigned to user"),
    SYSTEM_ROLE_CANNOT_BE_MODIFIED(HttpStatus.BAD_REQUEST, "A033", "System role cannot be modified or deleted"),
    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "A034", "Permission not found"),
    ROLE_KEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "A039", "Role key already exists"),

    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "A035", "Membership not found"),
    MEMBERSHIP_TIER_NOT_FOUND(HttpStatus.NOT_FOUND, "A036", "Membership tier not found"),
    MEMBERSHIP_ALREADY_EXISTS(HttpStatus.CONFLICT, "A037", "Membership already exists for this service"),
    MEMBERSHIP_EXPIRED(HttpStatus.FORBIDDEN, "A038", "Membership has expired"),

    ROLE_INCLUDE_CYCLE_DETECTED(HttpStatus.BAD_REQUEST, "A043", "Adding this include would create a circular dependency"),
    ROLE_INCLUDE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A044", "Role include relationship already exists"),
    ROLE_INCLUDE_NOT_FOUND(HttpStatus.NOT_FOUND, "A045", "Role include relationship not found"),
    ROLE_INCLUDE_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "A046", "A role cannot include itself"),

    ROLE_DEFAULT_MAPPING_NOT_FOUND(HttpStatus.NOT_FOUND, "A047", "Role default membership mapping not found"),
    ROLE_DEFAULT_MAPPING_ALREADY_EXISTS(HttpStatus.CONFLICT, "A048", "Role default membership mapping already exists"),
    MEMBERSHIP_TIER_IN_USE(HttpStatus.CONFLICT, "A049", "Membership tier is in use and cannot be deleted"),
    MEMBERSHIP_TIER_ALREADY_EXISTS(HttpStatus.CONFLICT, "A050", "Membership tier already exists");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
