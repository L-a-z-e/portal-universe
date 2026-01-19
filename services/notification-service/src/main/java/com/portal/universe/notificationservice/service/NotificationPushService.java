package com.portal.universe.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public void push(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);

        // 1. Send directly via WebSocket
        messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(),
                "/queue/notifications",
                response
        );

        // 2. Also publish to Redis Pub/Sub for multi-instance support
        String channel = "notification:" + notification.getUserId();
        try {
            String jsonPayload = redisObjectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(channel, jsonPayload);
            log.debug("Published notification to Redis channel: {}", channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis", e);
        }
    }

    public void pushToAll(NotificationResponse response) {
        messagingTemplate.convertAndSend("/topic/notifications", response);
        log.debug("Broadcast notification to all users");
    }
}
