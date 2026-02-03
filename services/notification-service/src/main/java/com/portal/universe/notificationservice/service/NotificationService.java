package com.portal.universe.notificationservice.service;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Notification create(CreateNotificationCommand command);

    Page<NotificationResponse> getNotifications(String userId, Pageable pageable);

    Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable);

    long getUnreadCount(String userId);

    NotificationResponse markAsRead(Long notificationId, String userId);

    int markAllAsRead(String userId);

    void delete(Long notificationId, String userId);
}
