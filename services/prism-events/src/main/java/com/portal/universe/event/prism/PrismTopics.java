package com.portal.universe.event.prism;

/**
 * Prism 도메인 Kafka topic 상수.
 *
 * <p>SSOT: {@code services/event-contracts/schemas/prism.task.*.schema.json}</p>
 * <p>NestJS 대응: {@code prism-service/src/modules/event/prism-topics.ts}</p>
 * <p>CI 검증: {@code scripts/validate-event-contracts.js}가 양측 일치를 확인한다.</p>
 */
public final class PrismTopics {

    public static final String TASK_COMPLETED = "prism.task.completed";
    public static final String TASK_FAILED = "prism.task.failed";

    private PrismTopics() {}
}
