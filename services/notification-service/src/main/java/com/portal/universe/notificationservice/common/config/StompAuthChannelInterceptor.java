package com.portal.universe.notificationservice.common.config;

import com.portal.universe.notificationservice.service.WsTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String TICKET_HEADER = "X-WS-Ticket";

    private final WsTicketService wsTicketService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String ticket = accessor.getFirstNativeHeader(TICKET_HEADER);
        String userId = wsTicketService.validateAndConsumeTicket(ticket);

        if (userId == null) {
            log.warn("WebSocket CONNECT rejected: invalid or expired ticket");
            throw new MessagingException("Invalid or expired WebSocket ticket");
        }

        accessor.setUser(new StompPrincipal(userId));
        log.debug("WebSocket CONNECT authenticated for user: {}", userId);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();

        if (user == null) {
            log.warn("WebSocket SUBSCRIBE rejected: no authenticated user");
            throw new MessagingException("Authentication required");
        }

        if (destination != null && destination.startsWith("/user/")) {
            String destUserId = extractUserIdFromDestination(destination);
            if (destUserId != null && !user.getName().equals(destUserId)) {
                log.warn("WebSocket SUBSCRIBE rejected: user {} attempted to subscribe to {}", user.getName(), destination);
                throw new MessagingException("Cannot subscribe to another user's queue");
            }
        }
    }

    private String extractUserIdFromDestination(String destination) {
        // /user/{userId}/queue/notifications → extract userId
        String[] parts = destination.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }
}
