package com.portal.universe.apigateway.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GatewayErrorCode {

    // Authentication
    AUTHENTICATION_REQUIRED("GW-A001", "Authentication required", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("GW-A002", "Access denied", HttpStatus.FORBIDDEN),
    TOKEN_REVOKED("GW-A003", "Token revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("GW-A004", "Token expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("GW-A005", "Invalid token", HttpStatus.UNAUTHORIZED),

    // Rate Limiting
    TOO_MANY_REQUESTS("GW-R001", "Too many requests", HttpStatus.TOO_MANY_REQUESTS),

    // Fallback (Circuit Breaker)
    AUTH_SERVICE_UNAVAILABLE("GW-F001",
            "인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    BLOG_SERVICE_UNAVAILABLE("GW-F002",
            "블로그 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    SHOPPING_SERVICE_UNAVAILABLE("GW-F003",
            "쇼핑 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    NOTIFICATION_SERVICE_UNAVAILABLE("GW-F004",
            "알림 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    SHOPPING_SELLER_SERVICE_UNAVAILABLE("GW-F005",
            "판매자 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    SHOPPING_SETTLEMENT_SERVICE_UNAVAILABLE("GW-F006",
            "정산 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    DRIVE_SERVICE_UNAVAILABLE("GW-F007",
            "드라이브 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    PRISM_SERVICE_UNAVAILABLE("GW-F008",
            "Prism 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE),
    CHATBOT_SERVICE_UNAVAILABLE("GW-F009",
            "챗봇 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
