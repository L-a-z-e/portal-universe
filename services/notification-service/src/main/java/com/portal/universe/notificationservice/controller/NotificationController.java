package com.portal.universe.notificationservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import com.portal.universe.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotifications(userId, pageable)));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUnreadNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadNotifications(userId, pageable)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(id, userId)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAllAsRead(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
