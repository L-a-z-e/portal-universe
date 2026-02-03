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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 도메인 이벤트를 알림 생성 커맨드로 변환하는 컨버터입니다.
 */
@Component
public class NotificationEventConverter {

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    // ===== Shopping Events =====

    public CreateNotificationCommand convert(OrderCreatedEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.ORDER_CREATED,
                "주문이 접수되었습니다",
                String.format("%d개 상품, %s원 결제 대기중",
                        event.itemCount(), formatPrice(event.totalAmount())),
                "/shopping/orders/" + event.orderNumber(),
                event.orderNumber(),
                "order"
        );
    }

    public CreateNotificationCommand convert(OrderCancelledEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.ORDER_CANCELLED,
                "주문이 취소되었습니다",
                String.format("주문번호: %s - %s", event.orderNumber(), event.cancelReason()),
                "/shopping/orders/" + event.orderNumber(),
                event.orderNumber(),
                "order"
        );
    }

    public CreateNotificationCommand convert(PaymentCompletedEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.PAYMENT_COMPLETED,
                "결제가 완료되었습니다",
                String.format("%s원 결제 완료", formatPrice(event.amount())),
                "/shopping/orders/" + event.orderNumber(),
                event.paymentNumber(),
                "payment"
        );
    }

    public CreateNotificationCommand convert(PaymentFailedEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.PAYMENT_FAILED,
                "결제가 실패했습니다",
                String.format("사유: %s", truncate(event.failureReason(), 50)),
                "/shopping/orders/" + event.orderNumber(),
                event.paymentNumber(),
                "payment"
        );
    }

    public CreateNotificationCommand convert(DeliveryShippedEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.DELIVERY_STARTED,
                "배송이 시작되었습니다",
                String.format("운송장번호: %s (%s)", event.trackingNumber(), event.carrier()),
                "/shopping/orders/" + event.orderNumber(),
                event.trackingNumber(),
                "delivery"
        );
    }

    public CreateNotificationCommand convert(CouponIssuedEvent event) {
        return new CreateNotificationCommand(
                String.valueOf(event.userId()),
                NotificationType.COUPON_ISSUED,
                "쿠폰이 발급되었습니다",
                String.format("%s - %s할인",
                        event.couponName(), formatDiscount(event.discountValue(), event.discountType())),
                "/shopping/coupons",
                event.couponCode(),
                "coupon"
        );
    }

    // ===== Blog Events =====

    public CreateNotificationCommand convert(PostLikedEvent event) {
        return new CreateNotificationCommand(
                event.authorId(),
                NotificationType.BLOG_LIKE,
                "게시글에 좋아요가 달렸습니다",
                String.format("\"%s\"에 %s님이 좋아요를 눌렀습니다",
                        truncate(event.postTitle(), 30), event.likerName()),
                "/blog/" + event.postId(),
                event.likeId(),
                "like"
        );
    }

    public CreateNotificationCommand convert(CommentCreatedEvent event) {
        return new CreateNotificationCommand(
                event.authorId(),
                NotificationType.BLOG_COMMENT,
                "게시글에 새 댓글이 달렸습니다",
                String.format("\"%s\"에 %s님이 댓글을 달았습니다: %s",
                        truncate(event.postTitle(), 20), event.commenterName(),
                        truncate(event.content(), 30)),
                "/blog/" + event.postId(),
                event.commentId(),
                "comment"
        );
    }

    public CreateNotificationCommand convert(CommentRepliedEvent event) {
        return new CreateNotificationCommand(
                event.parentCommentAuthorId(),
                NotificationType.BLOG_REPLY,
                "댓글에 답글이 달렸습니다",
                String.format("%s님이 회원님의 댓글에 답글을 달았습니다: %s",
                        event.replierName(), truncate(event.content(), 40)),
                "/blog/" + event.postId() + "#comment-" + event.parentCommentId(),
                event.replyId(),
                "reply"
        );
    }

    public CreateNotificationCommand convert(UserFollowedEvent event) {
        return new CreateNotificationCommand(
                event.followeeId(),
                NotificationType.BLOG_FOLLOW,
                "새 팔로워가 생겼습니다",
                String.format("%s님이 회원님을 팔로우하기 시작했습니다", event.followerName()),
                "/blog/users/" + event.followeeId() + "/followers",
                event.followId(),
                "follow"
        );
    }

    // ===== Prism Events =====

    public CreateNotificationCommand convert(PrismTaskCompletedEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.PRISM_TASK_COMPLETED,
                "AI 태스크가 완료되었습니다",
                String.format("\"%s\" 태스크가 %s 에이전트에 의해 완료되었습니다",
                        truncate(event.title(), 30), event.agentName()),
                "/prism/boards/" + event.boardId() + "/tasks/" + event.taskId(),
                String.valueOf(event.taskId()),
                "task"
        );
    }

    public CreateNotificationCommand convert(PrismTaskFailedEvent event) {
        return new CreateNotificationCommand(
                event.userId(),
                NotificationType.PRISM_TASK_FAILED,
                "AI 태스크가 실패했습니다",
                String.format("\"%s\" 태스크 실행 실패: %s",
                        truncate(event.title(), 25), truncate(event.errorMessage(), 30)),
                "/prism/boards/" + event.boardId() + "/tasks/" + event.taskId(),
                String.valueOf(event.taskId()),
                "task"
        );
    }

    // ===== Helper Methods =====

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        return PRICE_FORMAT.format(price);
    }

    private String formatDiscount(int value, String type) {
        return "PERCENTAGE".equals(type)
                ? value + "%"
                : formatPrice(BigDecimal.valueOf(value)) + "원";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
