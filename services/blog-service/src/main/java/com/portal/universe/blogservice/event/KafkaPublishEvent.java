package com.portal.universe.blogservice.event;

import org.apache.avro.specific.SpecificRecord;

/**
 * DB 트랜잭션 커밋 후 Kafka로 발행할 이벤트를 담는 Spring 도메인 이벤트.
 */
public record KafkaPublishEvent(String topic, String key, SpecificRecord payload) {}
