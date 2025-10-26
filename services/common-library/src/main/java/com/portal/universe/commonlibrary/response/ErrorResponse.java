package com.portal.universe.commonlibrary.response;

import lombok.Getter;

/**
 * API 실패 응답 시, 에러 정보를 담는 DTO 클래스입니다.
 * {@link ApiResponse} 클래스 내부에서 사용됩니다.
 */
@Getter
public class ErrorResponse {
    private final String code;    // 에러 코드 (예: "C001")
    private final String message; // 에러 메시지

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
