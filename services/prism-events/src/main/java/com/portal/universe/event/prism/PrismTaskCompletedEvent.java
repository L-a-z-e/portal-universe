package com.portal.universe.event.prism;

/**
 * Prism 서비스에서 AI 태스크가 완료될 때 발행되는 이벤트입니다.
 */
public record PrismTaskCompletedEvent(
        Integer taskId,
        Integer boardId,
        String userId,
        String title,
        String status,
        String agentName,
        Integer executionId,
        String timestamp
) {}
