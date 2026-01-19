package com.portal.universe.shoppingservice.queue.domain;

/**
 * Queue Entry Status
 */
public enum QueueStatus {
    WAITING,    // 대기 중
    ENTERED,    // 입장 완료
    EXPIRED,    // 만료
    LEFT        // 이탈
}
