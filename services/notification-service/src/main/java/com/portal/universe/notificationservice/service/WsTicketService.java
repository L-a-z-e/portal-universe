package com.portal.universe.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WsTicketService {

    private static final String TICKET_KEY_PREFIX = "ws-ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate stringRedisTemplate;

    public String createTicket(String userId) {
        String ticket = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(TICKET_KEY_PREFIX + ticket, userId, TICKET_TTL);
        return ticket;
    }

    public String validateAndConsumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        String key = TICKET_KEY_PREFIX + ticket;
        return stringRedisTemplate.opsForValue().getAndDelete(key);
    }
}
