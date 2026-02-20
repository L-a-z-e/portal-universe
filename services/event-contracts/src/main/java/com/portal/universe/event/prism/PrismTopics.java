package com.portal.universe.event.prism;

/**
 * Prism 도메인 Kafka topic 상수.
 *
 * <p>SSOT: {@code services/event-contracts/schemas/com/portal/universe/event/prism/*.avsc}</p>
 * <p>NestJS 대응: {@code prism-service/src/modules/event/prism-topics.ts}</p>
 */
public final class PrismTopics {

    public static final String TASK_COMPLETED = "prism.task.completed";
    public static final String TASK_FAILED = "prism.task.failed";

    private PrismTopics() {}
}
