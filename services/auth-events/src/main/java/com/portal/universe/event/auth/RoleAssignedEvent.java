package com.portal.universe.event.auth;

/**
 * 역할이 사용자에게 할당되었을 때 발행되는 이벤트 DTO.
 * auth-service 내부에서는 Spring ApplicationEvent로 처리하고,
 * 외부 서비스 구독용으로 Kafka를 통해 발행됩니다.
 *
 * @param userId 역할이 할당된 사용자 ID
 * @param roleKey 할당된 역할 키 (예: ROLE_USER, ROLE_SHOPPING_SELLER)
 * @param assignedBy 할당을 수행한 주체 (관리자 ID 또는 SYSTEM_REGISTRATION)
 * @param occurredAt 이벤트 발생 시각 (epoch millis)
 */
public record RoleAssignedEvent(
        String userId,
        String roleKey,
        String assignedBy,
        long occurredAt
) {
    public static RoleAssignedEvent of(String userId, String roleKey, String assignedBy) {
        return new RoleAssignedEvent(userId, roleKey, assignedBy, System.currentTimeMillis());
    }
}
