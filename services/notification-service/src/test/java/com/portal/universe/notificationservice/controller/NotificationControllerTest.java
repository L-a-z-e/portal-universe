package com.portal.universe.notificationservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import com.portal.universe.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private NotificationResponse sampleResponse;
    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final AuthUser AUTH_USER = new AuthUser(TEST_USER_ID, "Test User", "tester");

    @BeforeEach
    void setUp() {
        sampleResponse = NotificationResponse.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .type(NotificationType.ORDER_CREATED)
                .title("주문 접수")
                .message("주문이 접수되었습니다")
                .link("/orders/123")
                .status(NotificationStatus.UNREAD)
                .referenceId("ORD-123")
                .referenceType("order")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== Existing Tests (Preserved) =====

    @Test
    @DisplayName("should_returnNotifications_when_validUser")
    void should_returnNotifications_when_validUser() {
        // given
        given(notificationService.getNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse)));

        // when
        ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> result =
                notificationController.getNotifications(AUTH_USER, PageRequest.of(0, 20));

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getData().getItems()).hasSize(1);
        assertThat(result.getBody().getData().getItems().get(0).getTitle()).isEqualTo("주문 접수");
    }

    @Test
    @DisplayName("should_returnUnreadCount_when_validUser")
    void should_returnUnreadCount_when_validUser() {
        // given
        given(notificationService.getUnreadCount(TEST_USER_ID)).willReturn(5L);

        // when
        ResponseEntity<ApiResponse<Long>> result = notificationController.getUnreadCount(AUTH_USER);

        // then
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(5L);
    }

    @Test
    @DisplayName("should_markAllAsRead_when_validUser")
    void should_markAllAsRead_when_validUser() {
        // given
        given(notificationService.markAllAsRead(TEST_USER_ID)).willReturn(3);

        // when
        ResponseEntity<ApiResponse<Integer>> result = notificationController.markAllAsRead(AUTH_USER);

        // then
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(3);
    }

    @Test
    @DisplayName("should_deleteNotification_when_validUser")
    void should_deleteNotification_when_validUser() {
        // when
        ResponseEntity<ApiResponse<Void>> result = notificationController.delete(1L, AUTH_USER);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }

    // ===== New Tests =====

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotifications {

        @Test
        @DisplayName("should_getNotifications_with_paginationParams")
        void should_getNotifications_with_paginationParams() {
            // given
            Pageable pageable = PageRequest.of(1, 10);
            Page<NotificationResponse> page = new PageImpl<>(
                    List.of(sampleResponse), pageable, 11);

            given(notificationService.getNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                    .willReturn(page);

            // when
            ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> result =
                    notificationController.getNotifications(AUTH_USER, pageable);

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getPage()).isEqualTo(2);
            assertThat(result.getBody().getData().getSize()).isEqualTo(10);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(11);
        }

        @Test
        @DisplayName("should_wrapResponseWithApiResponse_when_getNotifications")
        void should_wrapResponseWithApiResponse_when_getNotifications() {
            // given
            given(notificationService.getNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleResponse)));

            // when
            ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> result =
                    notificationController.getNotifications(AUTH_USER, PageRequest.of(0, 20));

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isNotNull();
            assertThat(result.getBody().getError()).isNull();
        }

        @Test
        @DisplayName("should_callService_when_getNotifications")
        void should_callService_when_getNotifications() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(notificationService.getNotifications(eq(TEST_USER_ID), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            notificationController.getNotifications(AUTH_USER, pageable);

            // then
            verify(notificationService).getNotifications(TEST_USER_ID, pageable);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread")
    class GetUnreadNotifications {

        @Test
        @DisplayName("should_returnOk_when_getUnreadNotifications")
        void should_returnOk_when_getUnreadNotifications() {
            // given
            NotificationResponse unreadResponse = NotificationResponse.builder()
                    .id(2L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.BLOG_LIKE)
                    .title("좋아요")
                    .message("좋아요가 달렸습니다")
                    .status(NotificationStatus.UNREAD)
                    .build();

            given(notificationService.getUnreadNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(unreadResponse)));

            // when
            ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> result =
                    notificationController.getUnreadNotifications(AUTH_USER, PageRequest.of(0, 20));

            // then
            assertThat(result.getStatusCode().value()).isEqualTo(200);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should_callGetUnreadNotifications_when_unreadEndpoint")
        void should_callGetUnreadNotifications_when_unreadEndpoint() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(notificationService.getUnreadNotifications(eq(TEST_USER_ID), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            notificationController.getUnreadNotifications(AUTH_USER, pageable);

            // then
            verify(notificationService).getUnreadNotifications(TEST_USER_ID, pageable);
        }

        @Test
        @DisplayName("should_filterUnread_when_getUnreadNotifications")
        void should_filterUnread_when_getUnreadNotifications() {
            // given
            NotificationResponse unread = NotificationResponse.builder()
                    .id(3L)
                    .userId(TEST_USER_ID)
                    .status(NotificationStatus.UNREAD)
                    .type(NotificationType.SYSTEM)
                    .title("알림")
                    .message("메시지")
                    .build();

            given(notificationService.getUnreadNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(unread)));

            // when
            ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> result =
                    notificationController.getUnreadNotifications(AUTH_USER, PageRequest.of(0, 20));

            // then
            assertThat(result.getBody().getData().getItems()).allMatch(
                    r -> r.getStatus() == NotificationStatus.UNREAD);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread/count")
    class GetUnreadCount {

        @Test
        @DisplayName("should_returnUnreadCount_as_long")
        void should_returnUnreadCount_as_long() {
            // given
            given(notificationService.getUnreadCount(TEST_USER_ID)).willReturn(42L);

            // when
            ResponseEntity<ApiResponse<Long>> result = notificationController.getUnreadCount(AUTH_USER);

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData()).isEqualTo(42L);
            assertThat(result.getBody().getData()).isInstanceOf(Long.class);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("should_returnOk_when_markAsRead")
        void should_returnOk_when_markAsRead() {
            // given
            NotificationResponse readResponse = NotificationResponse.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.ORDER_CREATED)
                    .title("주문 접수")
                    .message("주문이 접수되었습니다")
                    .status(NotificationStatus.READ)
                    .readAt(LocalDateTime.now())
                    .build();

            given(notificationService.markAsRead(1L, TEST_USER_ID)).willReturn(readResponse);

            // when
            ResponseEntity<ApiResponse<NotificationResponse>> result =
                    notificationController.markAsRead(1L, AUTH_USER);

            // then
            assertThat(result.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("should_passIdAndUserId_when_markAsRead")
        void should_passIdAndUserId_when_markAsRead() {
            // given
            Long notificationId = 99L;
            AuthUser differentUser = new AuthUser("different-user", "Diff", "diff");

            NotificationResponse response = NotificationResponse.builder()
                    .id(notificationId)
                    .userId(differentUser.uuid())
                    .type(NotificationType.SYSTEM)
                    .title("알림")
                    .message("메시지")
                    .status(NotificationStatus.READ)
                    .build();

            given(notificationService.markAsRead(notificationId, differentUser.uuid())).willReturn(response);

            // when
            notificationController.markAsRead(notificationId, differentUser);

            // then
            verify(notificationService).markAsRead(notificationId, differentUser.uuid());
        }

        @Test
        @DisplayName("should_returnNotificationResponse_when_markAsRead")
        void should_returnNotificationResponse_when_markAsRead() {
            // given
            NotificationResponse readResponse = NotificationResponse.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.BLOG_COMMENT)
                    .title("새 댓글")
                    .message("댓글이 달렸습니다")
                    .status(NotificationStatus.READ)
                    .readAt(LocalDateTime.now())
                    .build();

            given(notificationService.markAsRead(1L, TEST_USER_ID)).willReturn(readResponse);

            // when
            ResponseEntity<ApiResponse<NotificationResponse>> result =
                    notificationController.markAsRead(1L, AUTH_USER);

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData()).isNotNull();
            assertThat(result.getBody().getData().getId()).isEqualTo(1L);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(result.getBody().getData().getReadAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("should_returnCount_when_markAllAsRead")
        void should_returnCount_when_markAllAsRead() {
            // given
            given(notificationService.markAllAsRead(TEST_USER_ID)).willReturn(7);

            // when
            ResponseEntity<ApiResponse<Integer>> result =
                    notificationController.markAllAsRead(AUTH_USER);

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData()).isEqualTo(7);
        }

        @Test
        @DisplayName("should_callMarkAllAsRead_when_putReadAll")
        void should_callMarkAllAsRead_when_putReadAll() {
            // given
            given(notificationService.markAllAsRead(TEST_USER_ID)).willReturn(0);

            // when
            notificationController.markAllAsRead(AUTH_USER);

            // then
            verify(notificationService).markAllAsRead(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{id}")
    class DeleteNotification {

        @Test
        @DisplayName("should_returnNullData_when_delete")
        void should_returnNullData_when_delete() {
            // when
            ResponseEntity<ApiResponse<Void>> result =
                    notificationController.delete(1L, AUTH_USER);

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData()).isNull();
        }

        @Test
        @DisplayName("should_returnSuccess_when_delete")
        void should_returnSuccess_when_delete() {
            // when
            ResponseEntity<ApiResponse<Void>> result =
                    notificationController.delete(5L, AUTH_USER);

            // then
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("should_callServiceDelete_when_delete")
        void should_callServiceDelete_when_delete() {
            // given
            Long notificationId = 42L;

            // when
            notificationController.delete(notificationId, AUTH_USER);

            // then
            verify(notificationService).delete(notificationId, TEST_USER_ID);
        }
    }
}
