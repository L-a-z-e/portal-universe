package com.portal.universe.notificationservice.service;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Notification create(Long userId, NotificationType type, String title, String message,
                        String link, String referenceId, String referenceType);

    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    Page<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    void delete(Long notificationId, Long userId);
}
