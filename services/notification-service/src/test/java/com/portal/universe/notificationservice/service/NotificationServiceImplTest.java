package com.portal.universe.notificationservice.service;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("should_createNotification_when_validCommand")
    void should_createNotification_when_validCommand() {
        // given
        CreateNotificationCommand command = new CreateNotificationCommand(
                1L, NotificationType.ORDER_CREATED, "주문 접수", "주문이 접수되었습니다",
                "/orders/123", "ORD-123", "order"
        );

        Notification savedNotification = Notification.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .link("/orders/123")
                .referenceId("ORD-123")
                .referenceType("order")
                .build();

        given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                "ORD-123", "order", 1L)).willReturn(false);
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        // when
        Notification result = notificationService.create(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("should_skipDuplicate_when_sameReferenceExists")
    void should_skipDuplicate_when_sameReferenceExists() {
        // given
        CreateNotificationCommand command = new CreateNotificationCommand(
                1L, NotificationType.ORDER_CREATED, "주문 접수", "주문이 접수되었습니다",
                "/orders/123", "ORD-123", "order"
        );

        Notification existingNotification = Notification.builder()
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .referenceId("ORD-123")
                .referenceType("order")
                .build();

        given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                "ORD-123", "order", 1L)).willReturn(true);
        given(notificationRepository.findByReferenceIdAndReferenceTypeAndUserId(
                "ORD-123", "order", 1L)).willReturn(Optional.of(existingNotification));

        // when
        Notification result = notificationService.create(command);

        // then
        assertThat(result).isEqualTo(existingNotification);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_skipIdempotencyCheck_when_referenceIdIsNull")
    void should_skipIdempotencyCheck_when_referenceIdIsNull() {
        // given
        CreateNotificationCommand command = new CreateNotificationCommand(
                1L, NotificationType.SYSTEM, "환영합니다!", "가입 감사합니다",
                null, null, null
        );

        Notification savedNotification = Notification.builder()
                .userId(1L)
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message("가입 감사합니다")
                .build();

        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        // when
        Notification result = notificationService.create(command);

        // then
        assertThat(result).isNotNull();
        verify(notificationRepository, never())
                .existsByReferenceIdAndReferenceTypeAndUserId(any(), any(), any());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("should_returnUnreadCount_when_userHasNotifications")
    void should_returnUnreadCount_when_userHasNotifications() {
        // given
        given(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.UNREAD))
                .willReturn(5L);

        // when
        long count = notificationService.getUnreadCount(1L);

        // then
        assertThat(count).isEqualTo(5L);
    }
}
