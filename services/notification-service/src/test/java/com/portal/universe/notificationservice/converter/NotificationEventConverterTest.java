package com.portal.universe.notificationservice.converter;

import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.blog.UserFollowedEvent;
import com.portal.universe.event.prism.PrismTaskCompletedEvent;
import com.portal.universe.event.prism.PrismTaskFailedEvent;
import com.portal.universe.event.shopping.*;
import com.portal.universe.notificationservice.domain.NotificationType;
import com.portal.universe.notificationservice.dto.CreateNotificationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationEventConverter")
class NotificationEventConverterTest {

    private NotificationEventConverter converter;

    @BeforeEach
    void setUp() {
        converter = new NotificationEventConverter();
    }

    @Nested
    @DisplayName("Shopping Events")
    class ShoppingEventsTest {

        @Test
        @DisplayName("OrderCreatedEvent를 ORDER_CREATED 타입 커맨드로 변환한다")
        void should_convertOrderCreated_toCommand() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    "ORD-20250701-001",
                    "user-123",
                    new BigDecimal("59000"),
                    3,
                    List.of(),
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-123");
            assertThat(command.type()).isEqualTo(NotificationType.ORDER_CREATED);
            assertThat(command.title()).isEqualTo("주문이 접수되었습니다");
            assertThat(command.message()).contains("3개 상품");
            assertThat(command.message()).contains("59,000원");
            assertThat(command.link()).isEqualTo("/shopping/orders/ORD-20250701-001");
            assertThat(command.referenceId()).isEqualTo("ORD-20250701-001");
            assertThat(command.referenceType()).isEqualTo("order");
        }

        @Test
        @DisplayName("OrderCancelledEvent를 ORDER_CANCELLED 타입 커맨드로 변환한다")
        void should_convertOrderCancelled_toCommand() {
            OrderCancelledEvent event = new OrderCancelledEvent(
                    "ORD-001",
                    "user-123",
                    new BigDecimal("30000"),
                    "고객 요청",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-123");
            assertThat(command.type()).isEqualTo(NotificationType.ORDER_CANCELLED);
            assertThat(command.title()).isEqualTo("주문이 취소되었습니다");
            assertThat(command.message()).contains("ORD-001");
            assertThat(command.message()).contains("고객 요청");
            assertThat(command.link()).isEqualTo("/shopping/orders/ORD-001");
            assertThat(command.referenceId()).isEqualTo("ORD-001");
            assertThat(command.referenceType()).isEqualTo("order");
        }

        @Test
        @DisplayName("PaymentCompletedEvent를 PAYMENT_COMPLETED 타입 커맨드로 변환한다")
        void should_convertPaymentCompleted_toCommand() {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    "PAY-001",
                    "ORD-001",
                    "user-123",
                    new BigDecimal("150000"),
                    "CARD",
                    "PG-TX-001",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-123");
            assertThat(command.type()).isEqualTo(NotificationType.PAYMENT_COMPLETED);
            assertThat(command.title()).isEqualTo("결제가 완료되었습니다");
            assertThat(command.message()).contains("150,000원");
            assertThat(command.link()).isEqualTo("/shopping/orders/ORD-001");
            assertThat(command.referenceId()).isEqualTo("PAY-001");
            assertThat(command.referenceType()).isEqualTo("payment");
        }

        @Test
        @DisplayName("PaymentFailedEvent에서 실패 사유가 긴 경우 50자로 잘린다")
        void should_truncateFailureReason_when_paymentFailed() {
            String longReason = "A".repeat(60); // 60자
            PaymentFailedEvent event = new PaymentFailedEvent(
                    "PAY-002",
                    "ORD-002",
                    "user-456",
                    new BigDecimal("50000"),
                    "CARD",
                    longReason,
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-456");
            assertThat(command.type()).isEqualTo(NotificationType.PAYMENT_FAILED);
            assertThat(command.title()).isEqualTo("결제가 실패했습니다");
            // truncate(longReason, 50) = "AAA...AAA..." (50자 + "...")
            assertThat(command.message()).contains("A".repeat(50) + "...");
            assertThat(command.link()).isEqualTo("/shopping/orders/ORD-002");
            assertThat(command.referenceId()).isEqualTo("PAY-002");
            assertThat(command.referenceType()).isEqualTo("payment");
        }

        @Test
        @DisplayName("DeliveryShippedEvent를 DELIVERY_STARTED 타입 커맨드로 변환한다")
        void should_convertDeliveryShipped_toCommand() {
            DeliveryShippedEvent event = new DeliveryShippedEvent(
                    "TRACK-001",
                    "ORD-001",
                    "user-123",
                    "CJ대한통운",
                    LocalDate.of(2025, 7, 5),
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-123");
            assertThat(command.type()).isEqualTo(NotificationType.DELIVERY_STARTED);
            assertThat(command.title()).isEqualTo("배송이 시작되었습니다");
            assertThat(command.message()).contains("TRACK-001");
            assertThat(command.message()).contains("CJ대한통운");
            assertThat(command.link()).isEqualTo("/shopping/orders/ORD-001");
            assertThat(command.referenceId()).isEqualTo("TRACK-001");
            assertThat(command.referenceType()).isEqualTo("delivery");
        }

        @Test
        @DisplayName("CouponIssuedEvent (PERCENTAGE 타입)를 올바르게 변환한다")
        void should_convertCouponIssued_percentageType() {
            CouponIssuedEvent event = new CouponIssuedEvent(
                    "100",
                    "COUPON-001",
                    "여름 세일 쿠폰",
                    "PERCENTAGE",
                    15,
                    LocalDateTime.now().plusDays(30)
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("100");
            assertThat(command.type()).isEqualTo(NotificationType.COUPON_ISSUED);
            assertThat(command.title()).isEqualTo("쿠폰이 발급되었습니다");
            assertThat(command.message()).contains("여름 세일 쿠폰");
            assertThat(command.message()).contains("15%");
            assertThat(command.link()).isEqualTo("/shopping/coupons");
            assertThat(command.referenceId()).isEqualTo("COUPON-001");
            assertThat(command.referenceType()).isEqualTo("coupon");
        }

        @Test
        @DisplayName("CouponIssuedEvent (FIXED 타입)를 올바르게 변환한다")
        void should_convertCouponIssued_fixedType() {
            CouponIssuedEvent event = new CouponIssuedEvent(
                    "200",
                    "COUPON-002",
                    "신규 가입 쿠폰",
                    "FIXED",
                    5000,
                    LocalDateTime.now().plusDays(14)
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("200");
            assertThat(command.type()).isEqualTo(NotificationType.COUPON_ISSUED);
            assertThat(command.message()).contains("신규 가입 쿠폰");
            assertThat(command.message()).contains("5,000원");
        }

        @Test
        @DisplayName("CouponIssuedEvent의 userId가 String으로 전달된다")
        void should_convertLongUserId_toString() {
            CouponIssuedEvent event = new CouponIssuedEvent(
                    "999",
                    "COUPON-003",
                    "테스트 쿠폰",
                    "PERCENTAGE",
                    10,
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("999");
        }
    }

    @Nested
    @DisplayName("Blog Events")
    class BlogEventsTest {

        @Test
        @DisplayName("PostLikedEvent에서 authorId를 userId로 사용한다")
        void should_useAuthorIdAsUserId_forPostLiked() {
            PostLikedEvent event = new PostLikedEvent(
                    "like-001",
                    "post-123",
                    "나의 첫 블로그 글",
                    "author-user-id",
                    "liker-user-id",
                    "홍길동",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("author-user-id");
            assertThat(command.type()).isEqualTo(NotificationType.BLOG_LIKE);
            assertThat(command.title()).isEqualTo("게시글에 좋아요가 달렸습니다");
            assertThat(command.message()).contains("나의 첫 블로그 글");
            assertThat(command.message()).contains("홍길동");
            assertThat(command.link()).isEqualTo("/blog/post-123");
            assertThat(command.referenceId()).isEqualTo("like-001");
            assertThat(command.referenceType()).isEqualTo("like");
        }

        @Test
        @DisplayName("CommentCreatedEvent에서 content가 긴 경우 30자로 잘린다")
        void should_truncateContent_forCommentCreated() {
            String longContent = "이것은 매우 긴 댓글 내용입니다. 테스트를 위해 30자 이상으로 작성합니다.";
            CommentCreatedEvent event = new CommentCreatedEvent(
                    "comment-001",
                    "post-123",
                    "블로그 제목",
                    "author-id",
                    "commenter-id",
                    "김철수",
                    longContent,
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("author-id");
            assertThat(command.type()).isEqualTo(NotificationType.BLOG_COMMENT);
            assertThat(command.title()).isEqualTo("게시글에 새 댓글이 달렸습니다");
            assertThat(command.message()).contains("김철수");
            // content is truncated to 30 chars + "..."
            assertThat(command.message()).contains("...");
            assertThat(command.link()).isEqualTo("/blog/post-123");
            assertThat(command.referenceId()).isEqualTo("comment-001");
            assertThat(command.referenceType()).isEqualTo("comment");
        }

        @Test
        @DisplayName("CommentRepliedEvent에서 anchor 링크가 생성되고 parentCommentAuthorId가 userId로 사용된다")
        void should_createAnchorLink_forCommentReplied() {
            CommentRepliedEvent event = new CommentRepliedEvent(
                    "reply-001",
                    "post-456",
                    "parent-comment-id",
                    "parent-author-id",
                    "replier-id",
                    "이영희",
                    "짧은 답글",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("parent-author-id");
            assertThat(command.type()).isEqualTo(NotificationType.BLOG_REPLY);
            assertThat(command.title()).isEqualTo("댓글에 답글이 달렸습니다");
            assertThat(command.message()).contains("이영희");
            assertThat(command.message()).contains("짧은 답글");
            assertThat(command.link()).isEqualTo("/blog/post-456#comment-parent-comment-id");
            assertThat(command.referenceId()).isEqualTo("reply-001");
            assertThat(command.referenceType()).isEqualTo("reply");
        }

        @Test
        @DisplayName("UserFollowedEvent에서 followeeId를 userId로 사용한다")
        void should_useFolloweeIdAsUserId_forUserFollowed() {
            UserFollowedEvent event = new UserFollowedEvent(
                    "follow-001",
                    "followee-user-id",
                    "follower-user-id",
                    "팔로워이름",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("followee-user-id");
            assertThat(command.type()).isEqualTo(NotificationType.BLOG_FOLLOW);
            assertThat(command.title()).isEqualTo("새 팔로워가 생겼습니다");
            assertThat(command.message()).contains("팔로워이름");
            assertThat(command.link()).isEqualTo("/blog/users/followee-user-id/followers");
            assertThat(command.referenceId()).isEqualTo("follow-001");
            assertThat(command.referenceType()).isEqualTo("follow");
        }
    }

    @Nested
    @DisplayName("Prism Events")
    class PrismEventsTest {

        @Test
        @DisplayName("PrismTaskCompletedEvent를 올바른 링크 형식으로 변환한다")
        void should_convertTaskCompleted_withCorrectLinkFormat() {
            PrismTaskCompletedEvent event = new PrismTaskCompletedEvent(
                    42,
                    10,
                    "user-123",
                    "이미지 분석 태스크",
                    "COMPLETED",
                    "GPT-4 Agent",
                    100,
                    "2025-07-01T10:00:00"
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-123");
            assertThat(command.type()).isEqualTo(NotificationType.PRISM_TASK_COMPLETED);
            assertThat(command.title()).isEqualTo("AI 태스크가 완료되었습니다");
            assertThat(command.message()).contains("이미지 분석 태스크");
            assertThat(command.message()).contains("GPT-4 Agent");
            assertThat(command.link()).isEqualTo("/prism/boards/10/tasks/42");
            assertThat(command.referenceId()).isEqualTo("42");
            assertThat(command.referenceType()).isEqualTo("task");
        }

        @Test
        @DisplayName("PrismTaskFailedEvent에서 errorMessage가 긴 경우 잘린다")
        void should_truncateErrorMessage_forTaskFailed() {
            String longError = "E".repeat(40); // 40자 > maxLength 30
            PrismTaskFailedEvent event = new PrismTaskFailedEvent(
                    55,
                    20,
                    "user-456",
                    "데이터 처리 태스크",
                    "FAILED",
                    "Claude Agent",
                    200,
                    longError,
                    "2025-07-01T12:00:00"
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.userId()).isEqualTo("user-456");
            assertThat(command.type()).isEqualTo(NotificationType.PRISM_TASK_FAILED);
            assertThat(command.title()).isEqualTo("AI 태스크가 실패했습니다");
            assertThat(command.message()).contains("E".repeat(30) + "...");
            assertThat(command.link()).isEqualTo("/prism/boards/20/tasks/55");
            assertThat(command.referenceId()).isEqualTo("55");
            assertThat(command.referenceType()).isEqualTo("task");
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodsTest {

        @Test
        @DisplayName("truncate에 null이 전달되면 빈 문자열이 반환된다")
        void should_returnEmptyString_when_truncateNull() {
            // truncate(null) returns "" - PostLikedEvent의 postTitle이 null인 경우 검증
            PostLikedEvent event = new PostLikedEvent(
                    "like-001",
                    "post-123",
                    null, // null postTitle
                    "author-id",
                    "liker-id",
                    "홍길동",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            // truncate(null, 30) returns ""
            assertThat(command.message()).contains("\"\"");
        }

        @Test
        @DisplayName("truncate에 maxLength 이하 문자열은 그대로 반환된다")
        void should_returnOriginalString_when_withinMaxLength() {
            String shortTitle = "짧은 제목";
            PostLikedEvent event = new PostLikedEvent(
                    "like-002",
                    "post-456",
                    shortTitle,
                    "author-id",
                    "liker-id",
                    "김철수",
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.message()).contains("짧은 제목");
            assertThat(command.message()).doesNotContain("...");
        }

        @Test
        @DisplayName("formatPrice에 null이 전달되면 '0'이 반환된다")
        void should_returnZero_when_priceIsNull() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    "ORD-NULL",
                    "user-123",
                    null, // null totalAmount
                    2,
                    List.of(),
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.message()).contains("0원");
        }

        @Test
        @DisplayName("PaymentFailedEvent에서 failureReason이 50자 이하이면 잘리지 않는다")
        void should_notTruncate_when_failureReasonIsShort() {
            String shortReason = "카드 잔액 부족";
            PaymentFailedEvent event = new PaymentFailedEvent(
                    "PAY-003",
                    "ORD-003",
                    "user-789",
                    new BigDecimal("10000"),
                    "CARD",
                    shortReason,
                    LocalDateTime.now()
            );

            CreateNotificationCommand command = converter.convert(event);

            assertThat(command.message()).contains("카드 잔액 부족");
            assertThat(command.message()).doesNotContain("...");
        }
    }
}
