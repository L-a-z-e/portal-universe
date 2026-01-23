package com.portal.universe.commonlibrary.security.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 보안 감사 이벤트를 나타내는 클래스입니다.
 * 로그인, 접근 제어, 관리자 작업 등 보안 관련 모든 이벤트를 추적합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditEvent {

    /**
     * 고유 이벤트 ID (UUID)
     */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * 이벤트 유형
     */
    private SecurityAuditEventType eventType;

    /**
     * 이벤트 발생 시각
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 사용자 ID (시스템 내부 식별자)
     */
    private String userId;

    /**
     * 사용자명 (로그인 계정명)
     */
    private String username;

    /**
     * 클라이언트 IP 주소
     */
    private String ipAddress;

    /**
     * 사용자 에이전트 (브라우저 정보)
     */
    private String userAgent;

    /**
     * 요청 URI
     */
    private String requestUri;

    /**
     * HTTP 메서드 (GET, POST, PUT, DELETE 등)
     */
    private String requestMethod;

    /**
     * 추가 상세 정보 (자유 형식)
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    /**
     * 작업 성공 여부
     */
    private boolean success;

    /**
     * 실패 시 에러 메시지
     */
    private String errorMessage;

    /**
     * 추가 정보를 details 맵에 추가하는 헬퍼 메서드입니다.
     *
     * @param key   키
     * @param value 값
     */
    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

    /**
     * 작업 성공 여부를 설정합니다.
     *
     * @param success 성공 여부
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 에러 메시지를 설정합니다.
     *
     * @param errorMessage 에러 메시지
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * JSON 직렬화를 위한 문자열 표현을 반환합니다.
     * 로그 출력 시 사용됩니다.
     */
    @Override
    public String toString() {
        return String.format(
                "SecurityAuditEvent{eventId='%s', eventType=%s, timestamp=%s, userId='%s', username='%s', " +
                "ipAddress='%s', requestUri='%s', requestMethod='%s', success=%s, errorMessage='%s', details=%s}",
                eventId, eventType, timestamp, userId, username, ipAddress, requestUri, requestMethod,
                success, errorMessage, details
        );
    }
}
