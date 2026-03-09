package com.portal.universe.commonlibrary.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @KafkaListener 메서드에 자동으로 처리 시간/에러 메트릭을 수집하는 AOP Aspect.
 * <p>
 * 발행 메트릭:
 * - kafka_consumer_processing_seconds: 메시지 처리 시간 (Timer)
 * - kafka_consumer_errors_total: 처리 실패 카운터 (Counter)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(MeterRegistry.class)
public class KafkaConsumerMetricsAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(kafkaListener)")
    public Object measureProcessingTime(ProceedingJoinPoint joinPoint, KafkaListener kafkaListener) throws Throwable {
        String topic = extractTopic(kafkaListener);
        String groupId = kafkaListener.groupId().isEmpty() ? "default" : kafkaListener.groupId();
        String method = extractMethodName(joinPoint);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder("kafka_consumer_processing_seconds")
                    .tag("topic", topic)
                    .tag("consumer_group", groupId)
                    .tag("method", method)
                    .tag("status", "success")
                    .description("Kafka consumer message processing time")
                    .register(meterRegistry));
            return result;
        } catch (Throwable ex) {
            sample.stop(Timer.builder("kafka_consumer_processing_seconds")
                    .tag("topic", topic)
                    .tag("consumer_group", groupId)
                    .tag("method", method)
                    .tag("status", "error")
                    .description("Kafka consumer message processing time")
                    .register(meterRegistry));

            Counter.builder("kafka_consumer_errors_total")
                    .tag("topic", topic)
                    .tag("consumer_group", groupId)
                    .tag("exception", ex.getClass().getSimpleName())
                    .description("Kafka consumer processing errors")
                    .register(meterRegistry)
                    .increment();

            throw ex;
        }
    }

    private String extractTopic(KafkaListener kafkaListener) {
        String[] topics = kafkaListener.topics();
        if (topics.length > 0) {
            String topic = topics[0];
            // SpEL 표현식이면 "unknown"으로 대체 (런타임에 해석 불가)
            if (topic.startsWith("${") || topic.startsWith("#{")) {
                return "unknown";
            }
            return topic;
        }
        return "unknown";
    }

    private String extractMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
