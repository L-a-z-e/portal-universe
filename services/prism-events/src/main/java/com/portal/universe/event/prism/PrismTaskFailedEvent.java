package com.portal.universe.event.prism;

/**
 * Prism 서비스에서 AI 태스크가 실패할 때 발행되는 이벤트입니다.
 */
public record PrismTaskFailedEvent(
        Integer taskId,
        Integer boardId,
        String userId,
        String title,
        String status,
        String agentName,
        Integer executionId,
        String errorMessage,
        String timestamp
) {}
