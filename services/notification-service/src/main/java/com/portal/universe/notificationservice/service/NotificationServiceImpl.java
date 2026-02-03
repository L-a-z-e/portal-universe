package com.portal.universe.notificationservice.service;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import com.portal.universe.notificationservice.common.exception.NotificationErrorCode;
import com.portal.universe.notificationservice.repository.NotificationRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification create(CreateNotificationCommand cmd) {
        if (cmd.referenceId() != null && cmd.referenceType() != null) {
            boolean exists = notificationRepository
                    .existsByReferenceIdAndReferenceTypeAndUserId(
                        cmd.referenceId(), cmd.referenceType(), cmd.userId());
            if (exists) {
                log.info("Duplicate notification skipped: ref={}/{}, userId={}",
                        cmd.referenceType(), cmd.referenceId(), cmd.userId());
                return notificationRepository
                        .findByReferenceIdAndReferenceTypeAndUserId(
                            cmd.referenceId(), cmd.referenceType(), cmd.userId())
                        .orElseThrow();
            }
        }

        Notification notification = Notification.builder()
                .userId(cmd.userId())
                .type(cmd.type())
                .title(cmd.title())
                .message(cmd.message())
                .link(cmd.link())
                .referenceId(cmd.referenceId())
                .referenceType(cmd.referenceType())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: userId={}, type={}, id={}", cmd.userId(), cmd.type(), saved.getId());
        return saved;
    }

    @Override
    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    public Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, NotificationStatus.UNREAD, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomBusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId, NotificationStatus.READ, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void delete(Long notificationId, String userId) {
        notificationRepository.deleteByUserIdAndId(userId, notificationId);
    }
}
