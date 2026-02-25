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
                event.getUserId(),
                NotificationType.ORDER_CREATED,
                "주문이 접수되었습니다",
                String.format("%d개 상품, %s원 결제 대기중",
                        event.getItemCount(), formatPrice(event.getTotalAmount())),
                "/shopping/orders/" + event.getOrderNumber(),
                event.getOrderNumber(),
                "order"
        );
    }

    public CreateNotificationCommand convert(OrderCancelledEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.ORDER_CANCELLED,
                "주문이 취소되었습니다",
                String.format("주문번호: %s - %s", event.getOrderNumber(), event.getCancelReason()),
                "/shopping/orders/" + event.getOrderNumber(),
                event.getOrderNumber(),
                "order"
        );
    }

    public CreateNotificationCommand convert(PaymentCompletedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.PAYMENT_COMPLETED,
                "결제가 완료되었습니다",
                String.format("%s원 결제 완료", formatPrice(event.getAmount())),
                "/shopping/orders/" + event.getOrderNumber(),
                event.getPaymentNumber(),
                "payment"
        );
    }

    public CreateNotificationCommand convert(PaymentFailedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.PAYMENT_FAILED,
                "결제가 실패했습니다",
                String.format("사유: %s", truncate(event.getFailureReason(), 50)),
                "/shopping/orders/" + event.getOrderNumber(),
                event.getPaymentNumber(),
                "payment"
        );
    }

    public CreateNotificationCommand convert(DeliveryShippedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.DELIVERY_STARTED,
                "배송이 시작되었습니다",
                String.format("운송장번호: %s (%s)", event.getTrackingNumber(), event.getCarrier()),
                "/shopping/orders/" + event.getOrderNumber(),
                event.getTrackingNumber(),
                "delivery"
        );
    }

    public CreateNotificationCommand convert(CouponIssuedEvent event) {
        return new CreateNotificationCommand(
                String.valueOf(event.getUserId()),
                NotificationType.COUPON_ISSUED,
                "쿠폰이 발급되었습니다",
                String.format("%s - %s할인",
                        event.getCouponName(), formatDiscount(event.getDiscountValue(), event.getDiscountType())),
                "/shopping/coupons",
                event.getCouponCode(),
                "coupon"
        );
    }

    // ===== Blog Events =====

    public CreateNotificationCommand convert(PostLikedEvent event) {
        return new CreateNotificationCommand(
                event.getAuthorId(),
                NotificationType.BLOG_LIKE,
                "게시글에 좋아요가 달렸습니다",
                String.format("\"%s\"에 %s님이 좋아요를 눌렀습니다",
                        truncate(event.getPostTitle(), 30), event.getLikerName()),
                "/blog/" + event.getPostId(),
                event.getLikeId(),
                "like"
        );
    }

    public CreateNotificationCommand convert(CommentCreatedEvent event) {
        return new CreateNotificationCommand(
                event.getAuthorId(),
                NotificationType.BLOG_COMMENT,
                "게시글에 새 댓글이 달렸습니다",
                String.format("\"%s\"에 %s님이 댓글을 달았습니다: %s",
                        truncate(event.getPostTitle(), 20), event.getCommenterName(),
                        truncate(event.getContent(), 30)),
                "/blog/" + event.getPostId(),
                event.getCommentId(),
                "comment"
        );
    }

    public CreateNotificationCommand convert(CommentRepliedEvent event) {
        return new CreateNotificationCommand(
                event.getParentCommentAuthorId(),
                NotificationType.BLOG_REPLY,
                "댓글에 답글이 달렸습니다",
                String.format("%s님이 회원님의 댓글에 답글을 달았습니다: %s",
                        event.getReplierName(), truncate(event.getContent(), 40)),
                "/blog/" + event.getPostId() + "#comment-" + event.getParentCommentId(),
                event.getReplyId(),
                "reply"
        );
    }

    public CreateNotificationCommand convert(UserFollowedEvent event) {
        return new CreateNotificationCommand(
                event.getFolloweeId(),
                NotificationType.BLOG_FOLLOW,
                "새 팔로워가 생겼습니다",
                String.format("%s님이 회원님을 팔로우하기 시작했습니다", event.getFollowerName()),
                "/blog/users/" + event.getFolloweeId() + "/followers",
                event.getFollowId(),
                "follow"
        );
    }

    // ===== Prism Events =====

    public CreateNotificationCommand convert(PrismTaskCompletedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.PRISM_TASK_COMPLETED,
                "AI 태스크가 완료되었습니다",
                String.format("\"%s\" 태스크가 %s 에이전트에 의해 완료되었습니다",
                        truncate(event.getTitle(), 30), event.getAgentName()),
                "/prism/boards/" + event.getBoardId() + "/tasks/" + event.getTaskId(),
                String.valueOf(event.getTaskId()),
                "task"
        );
    }

    public CreateNotificationCommand convert(PrismTaskFailedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.PRISM_TASK_FAILED,
                "AI 태스크가 실패했습니다",
                String.format("\"%s\" 태스크 실행 실패: %s",
                        truncate(event.getTitle(), 25), truncate(event.getErrorMessage(), 30)),
                "/prism/boards/" + event.getBoardId() + "/tasks/" + event.getTaskId(),
                String.valueOf(event.getTaskId()),
                "task"
        );
    }

    // ===== Drive Events =====

    public CreateNotificationCommand convert(com.portal.universe.event.drive.FileUploadedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.DRIVE_FILE_UPLOADED,
                "파일이 업로드되었습니다",
                String.format("\"%s\" 파일이 업로드되었습니다 (%s)",
                        truncate(event.getFileName(), 30), formatFileSize(event.getFileSize())),
                "/drive/files/" + event.getFileId(),
                event.getFileId(),
                "file"
        );
    }

    public CreateNotificationCommand convert(com.portal.universe.event.drive.FileDeletedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.DRIVE_FILE_DELETED,
                "파일이 삭제되었습니다",
                "파일이 삭제되었습니다",
                "/drive",
                event.getFileId(),
                "file"
        );
    }

    public CreateNotificationCommand convert(com.portal.universe.event.drive.FolderCreatedEvent event) {
        return new CreateNotificationCommand(
                event.getUserId(),
                NotificationType.DRIVE_FOLDER_CREATED,
                "폴더가 생성되었습니다",
                String.format("\"%s\" 폴더가 생성되었습니다", truncate(event.getFolderName(), 30)),
                "/drive/folders/" + event.getFolderId(),
                event.getFolderId(),
                "folder"
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

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
