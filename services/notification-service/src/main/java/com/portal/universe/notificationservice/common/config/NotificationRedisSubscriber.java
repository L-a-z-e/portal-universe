package com.portal.universe.notificationservice.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.notificationservice.common.constants.NotificationConstants;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;

    public void onMessage(String message, String pattern) {
        try {
            // Extract userId from channel (notification:{userId})
            String channel = pattern;
            if (channel.startsWith(NotificationConstants.REDIS_CHANNEL_PREFIX)) {
                String userId = channel.substring(NotificationConstants.REDIS_CHANNEL_PREFIX.length());

                NotificationResponse notification = redisObjectMapper.readValue(message, NotificationResponse.class);

                // Send to user-specific WebSocket queue
                messagingTemplate.convertAndSendToUser(
                        userId,
                        NotificationConstants.WS_QUEUE_NOTIFICATIONS,
                        notification
                );

                log.debug("Pushed notification to user {} via WebSocket", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis notification message", e);
        }
    }
}
