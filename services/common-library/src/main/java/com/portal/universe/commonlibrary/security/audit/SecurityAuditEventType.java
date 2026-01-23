package com.portal.universe.commonlibrary.security.audit;

/**
 * 보안 감사 이벤트의 유형을 정의하는 열거형 클래스입니다.
 * 로그인, 권한, 데이터 접근 등 보안 관련 주요 이벤트를 추적하는 데 사용됩니다.
 */
public enum SecurityAuditEventType {

    /**
     * 로그인 성공 이벤트
     */
    LOGIN_SUCCESS,

    /**
     * 로그인 실패 이벤트
     */
    LOGIN_FAILURE,

    /**
     * 로그아웃 이벤트
     */
    LOGOUT,

    /**
     * JWT 토큰 갱신 이벤트
     */
    TOKEN_REFRESH,

    /**
     * JWT 토큰 폐기 이벤트
     */
    TOKEN_REVOKED,

    /**
     * 비밀번호 변경 이벤트
     */
    PASSWORD_CHANGED,

    /**
     * 계정 잠금 이벤트
     */
    ACCOUNT_LOCKED,

    /**
     * 계정 잠금 해제 이벤트
     */
    ACCOUNT_UNLOCKED,

    /**
     * 접근 거부 이벤트 (권한 부족)
     */
    ACCESS_DENIED,

    /**
     * 권한 변경 이벤트
     */
    PERMISSION_CHANGED,

    /**
     * 민감한 데이터 접근 이벤트
     */
    SENSITIVE_DATA_ACCESS,

    /**
     * 관리자 작업 이벤트
     */
    ADMIN_ACTION
}
