package com.portal.universe.notificationservice.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정을 담당하는 클래스입니다.
 *
 * 주요 기능:
 * 1. Consumer Factory 설정 (역직렬화, 그룹 ID 등)
 * 2. Error Handler 설정 (재시도 + Dead Letter Queue)
 * 3. Container Factory 설정 (리스너 컨테이너)
 *
 * Dead Letter Queue (DLQ):
 * - 처리 실패한 메시지를 별도 토픽에 저장
 * - 원본 토픽명 + ".DLT" 접미사 (예: shopping.order.created.DLT)
 * - 나중에 실패 원인 분석 및 재처리 가능
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-group}")
    private String groupId;

    @Value("${app.kafka.retry.interval-ms:1000}")
    private long retryIntervalMs;

    @Value("${app.kafka.retry.max-attempts:3}")
    private long maxRetryAttempts;

    /**
     * Consumer Factory를 생성합니다.
     *
     * ErrorHandlingDeserializer를 사용하는 이유:
     * - 잘못된 형식의 메시지가 들어와도 Consumer가 중단되지 않음
     * - 역직렬화 실패 시 에러 핸들러로 위임
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Kafka 브로커 연결 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // ErrorHandlingDeserializer로 감싸서 역직렬화 에러 처리
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // 실제 역직렬화기 지정
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JSON 역직렬화 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.portal.universe.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Offset 관리 설정
        // earliest: 처음부터 읽기 (새 Consumer Group일 때)
        // latest: 최신 메시지부터 읽기
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 수동 커밋 모드 (처리 완료 후 커밋)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // ========================================
    // Producer 설정 (DLQ 발행용)
    // ========================================

    /**
     * Producer Factory를 생성합니다.
     * DLQ(Dead Letter Queue)로 실패한 메시지를 발행할 때 사용됩니다.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // 메시지 전송 신뢰성 설정
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate을 생성합니다.
     * DLQ 발행 및 일반 메시지 발행에 사용됩니다.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // ========================================
    // Error Handler 설정
    // ========================================

    /**
     * 에러 핸들러를 생성합니다.
     *
     * 동작 방식:
     * 1. 메시지 처리 실패 시 RETRY_INTERVAL_MS 간격으로 재시도
     * 2. MAX_RETRY_ATTEMPTS 횟수 초과 시 DLQ로 이동
     * 3. DLQ 토픽명: 원본 토픽 + ".DLT" (Dead Letter Topic)
     *
     * @param kafkaTemplate DLQ 발행용 KafkaTemplate
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Dead Letter Publishing Recoverer: 실패한 메시지를 DLQ로 발행
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    // DLQ 토픽명 결정: 원본토픽.DLT
                    String dlqTopic = record.topic() + ".DLT";
                    log.error("Message sent to DLQ: topic={}, key={}, error={}",
                            dlqTopic, record.key(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition());
                }
        );

        // FixedBackOff: 고정 간격 재시도
        // - interval: 재시도 간격 (ms)
        // - maxAttempts: 최대 재시도 횟수
        FixedBackOff backOff = new FixedBackOff(retryIntervalMs, maxRetryAttempts);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 재시도하지 않을 예외 타입 지정 (비즈니스 로직 오류)
        // 이런 예외는 재시도해도 실패하므로 바로 DLQ로 이동
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        // 재시도 시 로깅
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Retry attempt {} for topic={}, key={}, error={}",
                    deliveryAttempt, record.topic(), record.key(), ex.getMessage());
        });

        return errorHandler;
    }

    /**
     * Kafka Listener Container Factory를 생성합니다.
     *
     * @KafkaListener 어노테이션이 사용하는 팩토리입니다.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        // 수동 커밋 모드: 처리 완료 후 offset 커밋
        // AckMode.RECORD: 각 레코드 처리 후 커밋
        // AckMode.BATCH: 배치 처리 후 커밋 (기본값)
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD
        );

        return factory;
    }
}
