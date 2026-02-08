package com.portal.universe.notificationservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import com.portal.universe.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 서비스 REST 컨트롤러입니다.
 *
 * <p>모든 엔드포인트는 {@code @CurrentUser AuthUser}를 통해 인증된 사용자 정보를 주입받습니다.
 * API Gateway에서 JWT 검증 후 전달한 헤더를 기반으로 합니다.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @CurrentUser AuthUser user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                notificationService.getNotifications(user.uuid(), pageable))));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUnreadNotifications(
            @CurrentUser AuthUser user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                notificationService.getUnreadNotifications(user.uuid(), pageable))));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(user.uuid())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(id, user.uuid())));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAllAsRead(user.uuid())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @CurrentUser AuthUser user) {
        notificationService.delete(id, user.uuid());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
