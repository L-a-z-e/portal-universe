package com.portal.universe.notificationservice.consumer;

import com.portal.universe.event.auth.UserSignedUpEvent;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.blog.UserFollowedEvent;
import com.portal.universe.event.prism.PrismTaskCompletedEvent;
import com.portal.universe.event.prism.PrismTaskFailedEvent;
import com.portal.universe.event.shopping.*;
import com.portal.universe.notificationservice.converter.NotificationEventConverter;
import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import com.portal.universe.notificationservice.service.NotificationPushService;
import com.portal.universe.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPushService pushService;

    @Mock
    private NotificationEventConverter converter;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    // ===== Existing Tests (Preserved) =====

    @Test
    @DisplayName("should_createWelcomeNotification_when_userSignup")
    void should_createWelcomeNotification_when_userSignup() {
        // given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        UserSignedUpEvent event = new UserSignedUpEvent(userId, "hong@test.com", "홍길동");

        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.SYSTEM)
                .title("환영합니다!")
                .message("홍길동님, Portal Universe에 가입해주셔서 감사합니다.")
                .build();

        given(notificationService.create(any(CreateNotificationCommand.class)))
                .willReturn(notification);

        // when
        notificationConsumer.handleUserSignup(event);

        // then
        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).create(captor.capture());

        CreateNotificationCommand captured = captor.getValue();
        assertThat(captured.userId()).isEqualTo(userId);
        assertThat(captured.type()).isEqualTo(NotificationType.SYSTEM);
        assertThat(captured.title()).isEqualTo("환영합니다!");
        assertThat(captured.message()).contains("홍길동");

        verify(pushService).push(notification);
    }

    @Test
    @DisplayName("should_createNotification_when_orderCreated")
    void should_createNotification_when_orderCreated() {
        // given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        OrderCreatedEvent event = new OrderCreatedEvent(
                "ORD-001", userId, new java.math.BigDecimal("50000"), 2, java.util.List.of(), java.time.LocalDateTime.now()
        );

        CreateNotificationCommand cmd = new CreateNotificationCommand(
                userId, NotificationType.ORDER_CREATED, "주문이 접수되었습니다",
                "2개 상품, 50,000원 결제 대기중", "/shopping/orders/ORD-001", "ORD-001", "order"
        );

        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("주문이 접수되었습니다")
                .message("2개 상품, 50,000원 결제 대기중")
                .build();

        given(converter.convert(event)).willReturn(cmd);
        given(notificationService.create(any(CreateNotificationCommand.class)))
                .willReturn(notification);

        // when
        notificationConsumer.handleOrderCreated(event);

        // then
        verify(converter).convert(event);
        verify(notificationService).create(cmd);
        verify(pushService).push(notification);
    }

    // ===== New Tests =====

    @Nested
    @DisplayName("Auth Events")
    class AuthEvents {

        @Test
        @DisplayName("should_includeUserName_in_welcomeMessage")
        void should_includeUserName_in_welcomeMessage() {
            // given
            UserSignedUpEvent event = new UserSignedUpEvent(TEST_USER_ID, "test@test.com", "김테스트");

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.SYSTEM)
                    .build();

            given(notificationService.create(any(CreateNotificationCommand.class)))
                    .willReturn(notification);

            // when
            notificationConsumer.handleUserSignup(event);

            // then
            ArgumentCaptor<CreateNotificationCommand> captor =
                    ArgumentCaptor.forClass(CreateNotificationCommand.class);
            verify(notificationService).create(captor.capture());

            assertThat(captor.getValue().message())
                    .contains("김테스트")
                    .contains("Portal Universe에 가입해주셔서 감사합니다.");
        }

        @Test
        @DisplayName("should_setSystemNotificationType_when_userSignup")
        void should_setSystemNotificationType_when_userSignup() {
            // given
            UserSignedUpEvent event = new UserSignedUpEvent(TEST_USER_ID, "test@test.com", "테스터");

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.SYSTEM)
                    .build();

            given(notificationService.create(any(CreateNotificationCommand.class)))
                    .willReturn(notification);

            // when
            notificationConsumer.handleUserSignup(event);

            // then
            ArgumentCaptor<CreateNotificationCommand> captor =
                    ArgumentCaptor.forClass(CreateNotificationCommand.class);
            verify(notificationService).create(captor.capture());

            assertThat(captor.getValue().type()).isEqualTo(NotificationType.SYSTEM);
        }

        @Test
        @DisplayName("should_callValidateOnNotificationEvent_when_userSignup")
        void should_callValidateOnNotificationEvent_when_userSignup() {
            // given - valid event should pass validation and proceed
            UserSignedUpEvent event = new UserSignedUpEvent(TEST_USER_ID, "test@test.com", "테스터");

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.SYSTEM)
                    .build();

            given(notificationService.create(any(CreateNotificationCommand.class)))
                    .willReturn(notification);

            // when - should not throw because validate() passes
            notificationConsumer.handleUserSignup(event);

            // then - verify full chain completes
            verify(notificationService).create(any(CreateNotificationCommand.class));
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_throwException_when_signupUserIdIsNull")
        void should_throwException_when_signupUserIdIsNull() {
            // given - null userId will cause validate() to throw
            UserSignedUpEvent event = new UserSignedUpEvent(null, "test@test.com", "테스터");

            // when & then
            assertThatThrownBy(() -> notificationConsumer.handleUserSignup(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("userId is required");
        }
    }

    @Nested
    @DisplayName("Shopping Events")
    class ShoppingEvents {

        @Test
        @DisplayName("should_createNotification_when_orderCancelled")
        void should_createNotification_when_orderCancelled() {
            // given
            OrderCancelledEvent event = new OrderCancelledEvent(
                    "ORD-002", TEST_USER_ID, new BigDecimal("30000"),
                    "고객 요청", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.ORDER_CANCELLED,
                    "주문이 취소되었습니다", "주문번호: ORD-002 - 고객 요청",
                    "/shopping/orders/ORD-002", "ORD-002", "order"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.ORDER_CANCELLED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleOrderCancelled(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_paymentCompleted")
        void should_createNotification_when_paymentCompleted() {
            // given
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    "PAY-001", "ORD-001", TEST_USER_ID,
                    new BigDecimal("50000"), "CARD", "PG-TX-001", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PAYMENT_COMPLETED,
                    "결제가 완료되었습니다", "50,000원 결제 완료",
                    "/shopping/orders/ORD-001", "PAY-001", "payment"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PAYMENT_COMPLETED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePaymentCompleted(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_paymentFailed")
        void should_createNotification_when_paymentFailed() {
            // given
            PaymentFailedEvent event = new PaymentFailedEvent(
                    "PAY-002", "ORD-002", TEST_USER_ID,
                    new BigDecimal("30000"), "CARD", "잔액 부족", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PAYMENT_FAILED,
                    "결제가 실패했습니다", "사유: 잔액 부족",
                    "/shopping/orders/ORD-002", "PAY-002", "payment"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PAYMENT_FAILED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePaymentFailed(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_deliveryShipped")
        void should_createNotification_when_deliveryShipped() {
            // given
            DeliveryShippedEvent event = new DeliveryShippedEvent(
                    "TRK-001", "ORD-001", TEST_USER_ID,
                    "CJ대한통운", LocalDate.now().plusDays(2), LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.DELIVERY_STARTED,
                    "배송이 시작되었습니다", "운송장번호: TRK-001 (CJ대한통운)",
                    "/shopping/orders/ORD-001", "TRK-001", "delivery"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.DELIVERY_STARTED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleDeliveryShipped(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_couponIssued")
        void should_createNotification_when_couponIssued() {
            // given
            CouponIssuedEvent event = new CouponIssuedEvent(
                    "1", "COUPON-001", "신규 가입 쿠폰",
                    "PERCENTAGE", 10, LocalDateTime.now().plusDays(30)
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "1", NotificationType.COUPON_ISSUED,
                    "쿠폰이 발급되었습니다", "신규 가입 쿠폰 - 10%할인",
                    "/shopping/coupons", "COUPON-001", "coupon"
            );

            Notification notification = Notification.builder()
                    .userId("1")
                    .type(NotificationType.COUPON_ISSUED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleCouponIssued(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_notCreateNotification_when_timeDealStarted")
        void should_notCreateNotification_when_timeDealStarted() {
            // given
            TimeDealStartedEvent event = new TimeDealStartedEvent(
                    1L, "여름 세일", LocalDateTime.now(), LocalDateTime.now().plusHours(2)
            );

            // when
            notificationConsumer.handleTimeDealStarted(event);

            // then - TimeDeal is broadcast, no notification created
            verifyNoInteractions(notificationService);
            verifyNoInteractions(pushService);
            verifyNoInteractions(converter);
        }

        @Test
        @DisplayName("should_notCallPushService_when_timeDealStarted")
        void should_notCallPushService_when_timeDealStarted() {
            // given
            TimeDealStartedEvent event = new TimeDealStartedEvent(
                    2L, "겨울 세일", LocalDateTime.now(), LocalDateTime.now().plusHours(3)
            );

            // when
            notificationConsumer.handleTimeDealStarted(event);

            // then
            verify(pushService, never()).push(any());
        }

        @Test
        @DisplayName("should_propagateException_when_orderCancelledFails")
        void should_propagateException_when_orderCancelledFails() {
            // given
            OrderCancelledEvent event = new OrderCancelledEvent(
                    "ORD-003", TEST_USER_ID, new BigDecimal("10000"),
                    "품절", LocalDateTime.now()
            );

            given(converter.convert(event)).willThrow(new RuntimeException("Converter error"));

            // when & then
            assertThatThrownBy(() -> notificationConsumer.handleOrderCancelled(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Converter error");
        }

        @Test
        @DisplayName("should_propagateException_when_paymentCompletedFails")
        void should_propagateException_when_paymentCompletedFails() {
            // given
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    "PAY-003", "ORD-003", TEST_USER_ID,
                    new BigDecimal("20000"), "CARD", "PG-TX-003", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PAYMENT_COMPLETED,
                    "결제가 완료되었습니다", "20,000원 결제 완료",
                    "/shopping/orders/ORD-003", "PAY-003", "payment"
            );

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willThrow(new RuntimeException("Service error"));

            // when & then
            assertThatThrownBy(() -> notificationConsumer.handlePaymentCompleted(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Service error");
        }

        @Test
        @DisplayName("should_callConverterAndServiceAndPush_when_paymentFailed")
        void should_callConverterAndServiceAndPush_when_paymentFailed() {
            // given
            PaymentFailedEvent event = new PaymentFailedEvent(
                    "PAY-004", "ORD-004", TEST_USER_ID,
                    new BigDecimal("15000"), "CARD", "한도 초과", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PAYMENT_FAILED,
                    "결제가 실패했습니다", "사유: 한도 초과",
                    "/shopping/orders/ORD-004", "PAY-004", "payment"
            );

            Notification notification = Notification.builder()
                    .id(10L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PAYMENT_FAILED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePaymentFailed(event);

            // then - verify full chain: converter -> service -> push
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }
    }

    @Nested
    @DisplayName("Blog Events")
    class BlogEvents {

        @Test
        @DisplayName("should_createNotification_when_postLiked")
        void should_createNotification_when_postLiked() {
            // given
            PostLikedEvent event = new PostLikedEvent(
                    "LIKE-001", "POST-001", "테스트 게시글",
                    "author-001", "liker-001", "좋아요 누른 사람", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "author-001", NotificationType.BLOG_LIKE,
                    "게시글에 좋아요가 달렸습니다",
                    "\"테스트 게시글\"에 좋아요 누른 사람님이 좋아요를 눌렀습니다",
                    "/blog/POST-001", "LIKE-001", "like"
            );

            Notification notification = Notification.builder()
                    .userId("author-001")
                    .type(NotificationType.BLOG_LIKE)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePostLiked(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_commentCreated")
        void should_createNotification_when_commentCreated() {
            // given
            CommentCreatedEvent event = new CommentCreatedEvent(
                    "CMT-001", "POST-001", "테스트 게시글",
                    "author-001", "commenter-001", "댓글 작성자",
                    "좋은 글이네요!", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "author-001", NotificationType.BLOG_COMMENT,
                    "게시글에 새 댓글이 달렸습니다",
                    "\"테스트 게시글\"에 댓글 작성자님이 댓글을 달았습니다: 좋은 글이네요!",
                    "/blog/POST-001", "CMT-001", "comment"
            );

            Notification notification = Notification.builder()
                    .userId("author-001")
                    .type(NotificationType.BLOG_COMMENT)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleCommentCreated(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_commentReplied")
        void should_createNotification_when_commentReplied() {
            // given
            CommentRepliedEvent event = new CommentRepliedEvent(
                    "REPLY-001", "POST-001", "CMT-001",
                    "parent-author-001", "replier-001", "답글 작성자",
                    "감사합니다!", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "parent-author-001", NotificationType.BLOG_REPLY,
                    "댓글에 답글이 달렸습니다",
                    "답글 작성자님이 회원님의 댓글에 답글을 달았습니다: 감사합니다!",
                    "/blog/POST-001#comment-CMT-001", "REPLY-001", "reply"
            );

            Notification notification = Notification.builder()
                    .userId("parent-author-001")
                    .type(NotificationType.BLOG_REPLY)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleCommentReplied(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_userFollowed")
        void should_createNotification_when_userFollowed() {
            // given
            UserFollowedEvent event = new UserFollowedEvent(
                    "FOLLOW-001", "followee-001", "follower-001",
                    "팔로워", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "followee-001", NotificationType.BLOG_FOLLOW,
                    "새 팔로워가 생겼습니다",
                    "팔로워님이 회원님을 팔로우하기 시작했습니다",
                    "/blog/users/followee-001/followers", "FOLLOW-001", "follow"
            );

            Notification notification = Notification.builder()
                    .userId("followee-001")
                    .type(NotificationType.BLOG_FOLLOW)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleUserFollowed(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_propagateException_when_postLikedFails")
        void should_propagateException_when_postLikedFails() {
            // given
            PostLikedEvent event = new PostLikedEvent(
                    "LIKE-002", "POST-002", "게시글 2",
                    "author-002", "liker-002", "라이커", LocalDateTime.now()
            );

            given(converter.convert(event)).willThrow(new RuntimeException("Converter failed"));

            // when & then
            assertThatThrownBy(() -> notificationConsumer.handlePostLiked(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Converter failed");
        }

        @Test
        @DisplayName("should_callConverterAndServiceAndPush_when_commentCreated")
        void should_callConverterAndServiceAndPush_when_commentCreated() {
            // given
            CommentCreatedEvent event = new CommentCreatedEvent(
                    "CMT-002", "POST-002", "게시글 2",
                    "author-002", "commenter-002", "커멘터",
                    "댓글 내용", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "author-002", NotificationType.BLOG_COMMENT,
                    "게시글에 새 댓글이 달렸습니다", "내용",
                    "/blog/POST-002", "CMT-002", "comment"
            );

            Notification notification = Notification.builder()
                    .id(20L)
                    .userId("author-002")
                    .type(NotificationType.BLOG_COMMENT)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleCommentCreated(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }
    }

    @Nested
    @DisplayName("Prism Events")
    class PrismEvents {

        @Test
        @DisplayName("should_createNotification_when_prismTaskCompleted")
        void should_createNotification_when_prismTaskCompleted() {
            // given
            PrismTaskCompletedEvent event = new PrismTaskCompletedEvent(
                    1, 10, TEST_USER_ID, "이미지 분류",
                    "COMPLETED", "Claude Agent", 100, "2026-02-05T10:00:00"
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PRISM_TASK_COMPLETED,
                    "AI 태스크가 완료되었습니다",
                    "\"이미지 분류\" 태스크가 Claude Agent 에이전트에 의해 완료되었습니다",
                    "/prism/boards/10/tasks/1", "1", "task"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PRISM_TASK_COMPLETED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePrismTaskCompleted(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_createNotification_when_prismTaskFailed")
        void should_createNotification_when_prismTaskFailed() {
            // given
            PrismTaskFailedEvent event = new PrismTaskFailedEvent(
                    2, 10, TEST_USER_ID, "텍스트 분석",
                    "FAILED", "GPT Agent", 200, "timeout error", "2026-02-05T10:00:00"
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PRISM_TASK_FAILED,
                    "AI 태스크가 실패했습니다",
                    "\"텍스트 분석\" 태스크 실행 실패: timeout error",
                    "/prism/boards/10/tasks/2", "2", "task"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PRISM_TASK_FAILED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePrismTaskFailed(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }

        @Test
        @DisplayName("should_propagateException_when_prismTaskCompletedFails")
        void should_propagateException_when_prismTaskCompletedFails() {
            // given
            PrismTaskCompletedEvent event = new PrismTaskCompletedEvent(
                    3, 20, TEST_USER_ID, "작업",
                    "COMPLETED", "Agent", 300, "2026-02-05T10:00:00"
            );

            given(converter.convert(event)).willThrow(new RuntimeException("Task processing error"));

            // when & then
            assertThatThrownBy(() -> notificationConsumer.handlePrismTaskCompleted(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Task processing error");
        }

        @Test
        @DisplayName("should_callConverterAndServiceAndPush_when_prismTaskFailed")
        void should_callConverterAndServiceAndPush_when_prismTaskFailed() {
            // given
            PrismTaskFailedEvent event = new PrismTaskFailedEvent(
                    4, 20, TEST_USER_ID, "작업 2",
                    "FAILED", "Agent 2", 400, "memory error", "2026-02-05T10:00:00"
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.PRISM_TASK_FAILED,
                    "AI 태스크가 실패했습니다", "메시지",
                    "/prism/boards/20/tasks/4", "4", "task"
            );

            Notification notification = Notification.builder()
                    .id(30L)
                    .userId(TEST_USER_ID)
                    .type(NotificationType.PRISM_TASK_FAILED)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handlePrismTaskFailed(event);

            // then
            verify(converter).convert(event);
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }
    }

    @Nested
    @DisplayName("Chain Verification")
    class ChainVerification {

        @Test
        @DisplayName("should_callServiceCreate_with_converterResult_when_deliveryShipped")
        void should_callServiceCreate_with_converterResult_when_deliveryShipped() {
            // given
            DeliveryShippedEvent event = new DeliveryShippedEvent(
                    "TRK-002", "ORD-002", TEST_USER_ID,
                    "한진택배", LocalDate.now().plusDays(3), LocalDateTime.now()
            );

            CreateNotificationCommand expectedCmd = new CreateNotificationCommand(
                    TEST_USER_ID, NotificationType.DELIVERY_STARTED,
                    "배송이 시작되었습니다", "운송장번호: TRK-002 (한진택배)",
                    "/shopping/orders/ORD-002", "TRK-002", "delivery"
            );

            Notification notification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(NotificationType.DELIVERY_STARTED)
                    .build();

            given(converter.convert(event)).willReturn(expectedCmd);
            given(notificationService.create(expectedCmd)).willReturn(notification);

            // when
            notificationConsumer.handleDeliveryShipped(event);

            // then - verify service received exact command from converter
            verify(notificationService).create(expectedCmd);
        }

        @Test
        @DisplayName("should_callPushService_with_createdNotification_when_couponIssued")
        void should_callPushService_with_createdNotification_when_couponIssued() {
            // given
            CouponIssuedEvent event = new CouponIssuedEvent(
                    "2", "COUPON-002", "할인 쿠폰",
                    "FIXED", 5000, LocalDateTime.now().plusDays(7)
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "2", NotificationType.COUPON_ISSUED,
                    "쿠폰이 발급되었습니다", "할인 쿠폰 - 5,000원할인",
                    "/shopping/coupons", "COUPON-002", "coupon"
            );

            Notification createdNotification = Notification.builder()
                    .id(50L)
                    .userId("2")
                    .type(NotificationType.COUPON_ISSUED)
                    .title("쿠폰이 발급되었습니다")
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(createdNotification);

            // when
            notificationConsumer.handleCouponIssued(event);

            // then - verify push receives the exact notification from service.create
            verify(pushService).push(createdNotification);
        }

        @Test
        @DisplayName("should_callConverterConvert_with_event_when_userFollowed")
        void should_callConverterConvert_with_event_when_userFollowed() {
            // given
            UserFollowedEvent event = new UserFollowedEvent(
                    "FOLLOW-002", "followee-002", "follower-002",
                    "팔로워2", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "followee-002", NotificationType.BLOG_FOLLOW,
                    "새 팔로워가 생겼습니다", "메시지",
                    "/blog/users/followee-002/followers", "FOLLOW-002", "follow"
            );

            Notification notification = Notification.builder()
                    .userId("followee-002")
                    .type(NotificationType.BLOG_FOLLOW)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleUserFollowed(event);

            // then - verify converter received exact event
            ArgumentCaptor<UserFollowedEvent> captor =
                    ArgumentCaptor.forClass(UserFollowedEvent.class);
            verify(converter).convert(captor.capture());
            assertThat(captor.getValue()).isSameAs(event);
        }

        @Test
        @DisplayName("should_callCreateAndPush_inOrder_when_commentReplied")
        void should_callCreateAndPush_inOrder_when_commentReplied() {
            // given
            CommentRepliedEvent event = new CommentRepliedEvent(
                    "REPLY-002", "POST-003", "CMT-002",
                    "parent-author-002", "replier-002", "답글러",
                    "답글 내용", LocalDateTime.now()
            );

            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    "parent-author-002", NotificationType.BLOG_REPLY,
                    "댓글에 답글이 달렸습니다", "메시지",
                    "/blog/POST-003#comment-CMT-002", "REPLY-002", "reply"
            );

            Notification notification = Notification.builder()
                    .id(60L)
                    .userId("parent-author-002")
                    .type(NotificationType.BLOG_REPLY)
                    .build();

            given(converter.convert(event)).willReturn(cmd);
            given(notificationService.create(cmd)).willReturn(notification);

            // when
            notificationConsumer.handleCommentReplied(event);

            // then - both called
            verify(notificationService).create(cmd);
            verify(pushService).push(notification);
        }
    }
}
