package com.portal.universe.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.common.constants.NotificationConstants;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;

    public void push(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);

        // Redis Pub/Sub로 발행 → NotificationRedisSubscriber가 WebSocket 전달
        // 직접 WebSocket 전송 + Pub/Sub 동시 사용 시 동일 인스턴스에서 이중 전달됨
        String channel = NotificationConstants.REDIS_CHANNEL_PREFIX + notification.getUserId();
        try {
            String jsonPayload = redisObjectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(channel, jsonPayload);
            log.debug("Published notification to Redis channel: {}", channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis", e);
        }
    }

}
