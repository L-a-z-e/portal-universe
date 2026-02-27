package com.portal.universe.notificationservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import com.portal.universe.notificationservice.service.WsTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class WsTicketController {

    private final WsTicketService wsTicketService;

    @PostMapping("/ws-ticket")
    public ResponseEntity<ApiResponse<Map<String, String>>> createWsTicket(
            @CurrentUser AuthUser user) {
        String ticket = wsTicketService.createTicket(user.uuid());
        return ResponseEntity.ok(ApiResponse.success(Map.of("ticket", ticket)));
    }
}
