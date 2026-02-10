package com.portal.universe.event.prism;

/**
 * Prism 도메인 Kafka topic 상수.
 *
 * <p>주의: prism-service(NestJS)는 이 Java 클래스를 참조할 수 없으므로
 * topic 이름이 {@code kafka.producer.ts}에 하드코딩되어 있다.
 * topic 이름 변경 시 prism-service 코드도 함께 수정해야 한다.</p>
 */
public final class PrismTopics {

    public static final String TASK_COMPLETED = "prism.task.completed";
    public static final String TASK_FAILED = "prism.task.failed";

    private PrismTopics() {}
}
