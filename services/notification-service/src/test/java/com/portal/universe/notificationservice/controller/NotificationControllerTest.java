package com.portal.universe.notificationservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.NotificationResponse;
import com.portal.universe.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private NotificationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = NotificationResponse.builder()
                .id(1L)
                .userId(1L)
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

    @Test
    @DisplayName("should_returnNotifications_when_validUser")
    void should_returnNotifications_when_validUser() {
        // given
        given(notificationService.getNotifications(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse)));

        // when
        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<NotificationResponse>>> result =
                notificationController.getNotifications(1L, PageRequest.of(0, 20));

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getData().getContent()).hasSize(1);
        assertThat(result.getBody().getData().getContent().get(0).getTitle()).isEqualTo("주문 접수");
    }

    @Test
    @DisplayName("should_returnUnreadCount_when_validUser")
    void should_returnUnreadCount_when_validUser() {
        // given
        given(notificationService.getUnreadCount(1L)).willReturn(5L);

        // when
        ResponseEntity<ApiResponse<Long>> result = notificationController.getUnreadCount(1L);

        // then
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(5L);
    }

    @Test
    @DisplayName("should_markAllAsRead_when_validUser")
    void should_markAllAsRead_when_validUser() {
        // given
        given(notificationService.markAllAsRead(1L)).willReturn(3);

        // when
        ResponseEntity<ApiResponse<Integer>> result = notificationController.markAllAsRead(1L);

        // then
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(3);
    }

    @Test
    @DisplayName("should_deleteNotification_when_validUser")
    void should_deleteNotification_when_validUser() {
        // when
        ResponseEntity<ApiResponse<Void>> result = notificationController.delete(1L, 1L);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }
}
