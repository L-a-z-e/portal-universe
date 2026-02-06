package com.portal.universe.notificationservice.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.notificationservice.common.exception.NotificationErrorCode;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import com.portal.universe.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    // ===== Existing Tests (Preserved) =====

    @Test
    @DisplayName("should_createNotification_when_validCommand")
    void should_createNotification_when_validCommand() {
        // given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        CreateNotificationCommand command = new CreateNotificationCommand(
                userId, NotificationType.ORDER_CREATED, "주문 접수", "주문이 접수되었습니다",
                "/orders/123", "ORD-123", "order"
        );

        Notification savedNotification = Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .link("/orders/123")
                .referenceId("ORD-123")
                .referenceType("order")
                .build();

        given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                "ORD-123", "order", userId)).willReturn(false);
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        // when
        Notification result = notificationService.create(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("should_skipDuplicate_when_sameReferenceExists")
    void should_skipDuplicate_when_sameReferenceExists() {
        // given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        CreateNotificationCommand command = new CreateNotificationCommand(
                userId, NotificationType.ORDER_CREATED, "주문 접수", "주문이 접수되었습니다",
                "/orders/123", "ORD-123", "order"
        );

        Notification existingNotification = Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .referenceId("ORD-123")
                .referenceType("order")
                .build();

        given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                "ORD-123", "order", userId)).willReturn(true);
        given(notificationRepository.findByReferenceIdAndReferenceTypeAndUserId(
                "ORD-123", "order", userId)).willReturn(Optional.of(existingNotification));

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
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        CreateNotificationCommand command = new CreateNotificationCommand(
                userId, NotificationType.SYSTEM, "환영합니다!", "가입 감사합니다",
                null, null, null
        );

        Notification savedNotification = Notification.builder()
                .userId(userId)
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
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        given(notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD))
                .willReturn(5L);

        // when
        long count = notificationService.getUnreadCount(userId);

        // then
        assertThat(count).isEqualTo(5L);
    }

    // ===== New Tests =====

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_skipIdempotencyCheck_when_referenceTypeIsNull")
        void should_skipIdempotencyCheck_when_referenceTypeIsNull() {
            // given
            CreateNotificationCommand command = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.SYSTEM, "시스템 알림", "메시지",
                    null, "REF-001", null
            );

            Notification savedNotification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.SYSTEM)
                    .title("시스템 알림")
                    .message("메시지")
                    .referenceId("REF-001")
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
        @DisplayName("should_returnExistingNotification_when_duplicateDetected")
        void should_returnExistingNotification_when_duplicateDetected() {
            // given
            CreateNotificationCommand command = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PAYMENT_COMPLETED, "결제 완료", "결제가 완료되었습니다",
                    "/orders/123", "PAY-001", "payment"
            );

            Notification existingNotification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PAYMENT_COMPLETED)
                    .title("결제 완료")
                    .message("결제가 완료되었습니다")
                    .referenceId("PAY-001")
                    .referenceType("payment")
                    .status(NotificationStatus.UNREAD)
                    .build();

            given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                    "PAY-001", "payment", TEST_USER_ID)).willReturn(true);
            given(notificationRepository.findByReferenceIdAndReferenceTypeAndUserId(
                    "PAY-001", "payment", TEST_USER_ID)).willReturn(Optional.of(existingNotification));

            // when
            Notification result = notificationService.create(command);

            // then
            assertThat(result).isSameAs(existingNotification);
            assertThat(result.getReferenceId()).isEqualTo("PAY-001");
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_callRepositorySave_when_creatingNewNotification")
        void should_callRepositorySave_when_creatingNewNotification() {
            // given
            CreateNotificationCommand command = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.BLOG_LIKE, "좋아요", "좋아요가 달렸습니다",
                    "/blog/1", "LIKE-001", "like"
            );

            given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                    "LIKE-001", "like", TEST_USER_ID)).willReturn(false);
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            notificationService.create(command);

            // then
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(saved.getType()).isEqualTo(NotificationType.BLOG_LIKE);
            assertThat(saved.getTitle()).isEqualTo("좋아요");
            assertThat(saved.getMessage()).isEqualTo("좋아요가 달렸습니다");
            assertThat(saved.getLink()).isEqualTo("/blog/1");
            assertThat(saved.getReferenceId()).isEqualTo("LIKE-001");
            assertThat(saved.getReferenceType()).isEqualTo("like");
        }

        @Test
        @DisplayName("should_buildNotificationFromCommand_when_creating")
        void should_buildNotificationFromCommand_when_creating() {
            // given
            CreateNotificationCommand command = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.DELIVERY_STARTED, "배송 시작",
                    "배송이 시작되었습니다", "/shopping/orders/ORD-001",
                    "TRK-001", "delivery"
            );

            given(notificationRepository.existsByReferenceIdAndReferenceTypeAndUserId(
                    "TRK-001", "delivery", TEST_USER_ID)).willReturn(false);
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Notification result = notificationService.create(command);

            // then
            assertThat(result.getUserId()).isEqualTo(command.userId());
            assertThat(result.getType()).isEqualTo(command.type());
            assertThat(result.getTitle()).isEqualTo(command.title());
            assertThat(result.getMessage()).isEqualTo(command.message());
            assertThat(result.getLink()).isEqualTo(command.link());
            assertThat(result.getReferenceId()).isEqualTo(command.referenceId());
            assertThat(result.getReferenceType()).isEqualTo(command.referenceType());
        }

        @Test
        @DisplayName("should_skipIdempotencyCheck_when_bothReferenceFieldsAreNull")
        void should_skipIdempotencyCheck_when_bothReferenceFieldsAreNull() {
            // given
            CreateNotificationCommand command = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.SYSTEM, "알림", "메시지",
                    null, null, null
            );

            Notification saved = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.SYSTEM)
                    .title("알림")
                    .message("메시지")
                    .build();

            given(notificationRepository.save(any(Notification.class))).willReturn(saved);

            // when
            Notification result = notificationService.create(command);

            // then
            assertThat(result).isNotNull();
            verify(notificationRepository, never())
                    .existsByReferenceIdAndReferenceTypeAndUserId(any(), any(), any());
        }

        @Test
        @DisplayName("should_skipIdempotencyCheck_when_referenceIdIsNullButTypeIsNotNull")
        void should_skipIdempotencyCheck_when_referenceIdIsNullButTypeIsNotNull() {
            // given - referenceId is null but referenceType is not: condition is (null != null && "order" != null) = false
            CreateNotificationCommand command = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.ORDER_CREATED, "주문", "주문 접수",
                    "/orders/1", null, "order"
            );

            Notification saved = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.ORDER_CREATED)
                    .build();

            given(notificationRepository.save(any(Notification.class))).willReturn(saved);

            // when
            notificationService.create(command);

            // then
            verify(notificationRepository, never())
                    .existsByReferenceIdAndReferenceTypeAndUserId(any(), any(), any());
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("should_returnPaginatedNotifications_when_getNotifications")
        void should_returnPaginatedNotifications_when_getNotifications() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Notification notification = Notification.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.ORDER_CREATED)
                    .title("주문 접수")
                    .message("주문이 접수되었습니다")
                    .status(NotificationStatus.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID, pageable))
                    .willReturn(page);

            // when
            Page<NotificationResponse> result = notificationService.getNotifications(TEST_USER_ID, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should_mapToNotificationResponse_when_getNotifications")
        void should_mapToNotificationResponse_when_getNotifications() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Notification notification = Notification.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.BLOG_COMMENT)
                    .title("새 댓글")
                    .message("댓글이 달렸습니다")
                    .link("/blog/1")
                    .status(NotificationStatus.UNREAD)
                    .referenceId("CMT-001")
                    .referenceType("comment")
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID, pageable))
                    .willReturn(page);

            // when
            Page<NotificationResponse> result = notificationService.getNotifications(TEST_USER_ID, pageable);

            // then
            NotificationResponse response = result.getContent().get(0);
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getType()).isEqualTo(NotificationType.BLOG_COMMENT);
            assertThat(response.getTitle()).isEqualTo("새 댓글");
            assertThat(response.getMessage()).isEqualTo("댓글이 달렸습니다");
            assertThat(response.getLink()).isEqualTo("/blog/1");
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.UNREAD);
            assertThat(response.getReferenceId()).isEqualTo("CMT-001");
            assertThat(response.getReferenceType()).isEqualTo("comment");
        }

        @Test
        @DisplayName("should_filterUnreadNotifications_when_getUnreadNotifications")
        void should_filterUnreadNotifications_when_getUnreadNotifications() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Notification unread = Notification.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.SYSTEM)
                    .title("알림")
                    .message("메시지")
                    .status(NotificationStatus.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<Notification> page = new PageImpl<>(List.of(unread), pageable, 1);
            given(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    TEST_USER_ID, NotificationStatus.UNREAD, pageable)).willReturn(page);

            // when
            Page<NotificationResponse> result = notificationService.getUnreadNotifications(TEST_USER_ID, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(notificationRepository).findByUserIdAndStatusOrderByCreatedAtDesc(
                    TEST_USER_ID, NotificationStatus.UNREAD, pageable);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should_markAsRead_when_notificationExists")
        void should_markAsRead_when_notificationExists() {
            // given
            Long notificationId = 1L;
            Notification notification = Notification.builder()
                    .id(notificationId)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.ORDER_CREATED)
                    .title("주문 접수")
                    .message("주문이 접수되었습니다")
                    .status(NotificationStatus.UNREAD)
                    .build();

            given(notificationRepository.findByIdAndUserId(notificationId, TEST_USER_ID))
                    .willReturn(Optional.of(notification));

            // when
            NotificationResponse result = notificationService.markAsRead(notificationId, TEST_USER_ID);

            // then
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(notification.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("should_throwCustomBusinessException_when_notificationNotFound")
        void should_throwCustomBusinessException_when_notificationNotFound() {
            // given
            Long notificationId = 999L;
            given(notificationRepository.findByIdAndUserId(notificationId, TEST_USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, TEST_USER_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("should_returnNotificationResponse_when_markAsRead")
        void should_returnNotificationResponse_when_markAsRead() {
            // given
            Long notificationId = 1L;
            Notification notification = Notification.builder()
                    .id(notificationId)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.BLOG_LIKE)
                    .title("좋아요")
                    .message("좋아요가 달렸습니다")
                    .link("/blog/1")
                    .status(NotificationStatus.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(notificationRepository.findByIdAndUserId(notificationId, TEST_USER_ID))
                    .willReturn(Optional.of(notification));

            // when
            NotificationResponse result = notificationService.markAsRead(notificationId, TEST_USER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(notificationId);
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getType()).isEqualTo(NotificationType.BLOG_LIKE);
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should_returnUpdatedCount_when_markAllAsRead")
        void should_returnUpdatedCount_when_markAllAsRead() {
            // given
            given(notificationRepository.markAllAsRead(
                    eq(TEST_USER_ID), eq(NotificationStatus.READ), any(LocalDateTime.class)))
                    .willReturn(5);

            // when
            int count = notificationService.markAllAsRead(TEST_USER_ID);

            // then
            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("should_returnZero_when_noUnreadNotifications")
        void should_returnZero_when_noUnreadNotifications() {
            // given
            given(notificationRepository.markAllAsRead(
                    eq(TEST_USER_ID), eq(NotificationStatus.READ), any(LocalDateTime.class)))
                    .willReturn(0);

            // when
            int count = notificationService.markAllAsRead(TEST_USER_ID);

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("should_passCorrectStatusToRepository_when_markAllAsRead")
        void should_passCorrectStatusToRepository_when_markAllAsRead() {
            // given
            given(notificationRepository.markAllAsRead(
                    eq(TEST_USER_ID), eq(NotificationStatus.READ), any(LocalDateTime.class)))
                    .willReturn(3);

            // when
            notificationService.markAllAsRead(TEST_USER_ID);

            // then
            verify(notificationRepository).markAllAsRead(
                    eq(TEST_USER_ID), eq(NotificationStatus.READ), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should_deleteNotification_when_validIdAndUserId")
        void should_deleteNotification_when_validIdAndUserId() {
            // when
            notificationService.delete(1L, TEST_USER_ID);

            // then
            verify(notificationRepository).deleteByUserIdAndId(TEST_USER_ID, 1L);
        }

        @Test
        @DisplayName("should_callDeleteByUserIdAndId_when_delete")
        void should_callDeleteByUserIdAndId_when_delete() {
            // given
            Long notificationId = 42L;
            String userId = "another-user-id";

            // when
            notificationService.delete(notificationId, userId);

            // then
            verify(notificationRepository).deleteByUserIdAndId(userId, notificationId);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should_returnZero_when_noUnreadNotifications")
        void should_returnZero_when_noUnreadNotifications() {
            // given
            given(notificationRepository.countByUserIdAndStatus(TEST_USER_ID, NotificationStatus.UNREAD))
                    .willReturn(0L);

            // when
            long count = notificationService.getUnreadCount(TEST_USER_ID);

            // then
            assertThat(count).isEqualTo(0L);
        }
    }
}
