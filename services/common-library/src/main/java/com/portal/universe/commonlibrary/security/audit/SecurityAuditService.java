package com.portal.universe.commonlibrary.security.audit;

/**
 * 보안 감사 로그를 기록하는 서비스 인터페이스입니다.
 * 각 서비스에서 이 인터페이스를 주입받아 보안 관련 이벤트를 로깅할 수 있습니다.
 */
public interface SecurityAuditService {

    /**
     * 보안 감사 이벤트를 로그에 기록합니다.
     *
     * @param event 보안 감사 이벤트 객체
     */
    void log(SecurityAuditEvent event);

    /**
     * 로그인 성공 이벤트를 로그에 기록합니다.
     *
     * @param userId    사용자 ID
     * @param username  사용자명
     * @param ip        클라이언트 IP 주소
     * @param userAgent 사용자 에이전트
     */
    void logLoginSuccess(String userId, String username, String ip, String userAgent);

    /**
     * 로그인 실패 이벤트를 로그에 기록합니다.
     *
     * @param username 시도한 사용자명
     * @param ip       클라이언트 IP 주소
     * @param reason   실패 사유
     */
    void logLoginFailure(String username, String ip, String reason);

    /**
     * 로그아웃 이벤트를 로그에 기록합니다.
     *
     * @param userId 사용자 ID
     * @param ip     클라이언트 IP 주소
     */
    void logLogout(String userId, String ip);

    /**
     * 접근 거부 이벤트를 로그에 기록합니다.
     *
     * @param userId       사용자 ID
     * @param uri          접근 시도한 URI
     * @param requiredRole 필요한 권한
     */
    void logAccessDenied(String userId, String uri, String requiredRole);

    /**
     * 관리자 작업 이벤트를 로그에 기록합니다.
     *
     * @param adminId        관리자 ID
     * @param action         수행한 작업
     * @param targetResource 대상 리소스
     */
    void logAdminAction(String adminId, String action, String targetResource);

    /**
     * JWT 토큰 갱신 이벤트를 로그에 기록합니다.
     *
     * @param userId 사용자 ID
     * @param ip     클라이언트 IP 주소
     */
    void logTokenRefresh(String userId, String ip);

    /**
     * 비밀번호 변경 이벤트를 로그에 기록합니다.
     *
     * @param userId 사용자 ID
     * @param ip     클라이언트 IP 주소
     */
    void logPasswordChanged(String userId, String ip);

    /**
     * 민감한 데이터 접근 이벤트를 로그에 기록합니다.
     *
     * @param userId       사용자 ID
     * @param resourceType 리소스 유형
     * @param resourceId   리소스 ID
     */
    void logSensitiveDataAccess(String userId, String resourceType, String resourceId);
}
