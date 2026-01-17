package com.portal.universe.shoppingservice.delivery.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.domain.Address;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 배송 정보를 나타내는 JPA 엔티티입니다.
 */
@Entity
@Table(name = "deliveries", indexes = {
        @Index(name = "idx_delivery_tracking_number", columnList = "tracking_number", unique = true),
        @Index(name = "idx_delivery_order_id", columnList = "order_id"),
        @Index(name = "idx_delivery_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 운송장 번호
     */
    @Column(name = "tracking_number", nullable = false, unique = true, length = 30)
    private String trackingNumber;

    /**
     * 주문 ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 주문 번호
     */
    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    /**
     * 배송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    /**
     * 택배사
     */
    @Column(name = "carrier", length = 50)
    private String carrier;

    /**
     * 배송지 주소
     */
    @Embedded
    private Address shippingAddress;

    /**
     * 예상 배송일
     */
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    /**
     * 실제 배송 완료일
     */
    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    /**
     * 배송 이력
     */
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<DeliveryHistory> histories = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Delivery(Long orderId, String orderNumber, Address shippingAddress, String carrier) {
        this.trackingNumber = generateTrackingNumber();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = DeliveryStatus.PREPARING;
        this.shippingAddress = shippingAddress;
        this.carrier = carrier != null ? carrier : "기본택배";
        this.estimatedDeliveryDate = LocalDate.now().plusDays(3); // 기본 3일 후
    }

    /**
     * 배송 상태를 변경합니다.
     */
    public void updateStatus(DeliveryStatus newStatus, String location, String description) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_DELIVERY_STATUS);
        }

        this.status = newStatus;

        // 배송 완료 시 실제 배송일 기록
        if (newStatus == DeliveryStatus.DELIVERED) {
            this.actualDeliveryDate = LocalDate.now();
        }

        // 이력 추가
        addHistory(newStatus, location, description);
    }

    /**
     * 배송을 발송 처리합니다.
     */
    public void ship(String location, String description) {
        updateStatus(DeliveryStatus.SHIPPED, location, description);
    }

    /**
     * 배송 중 상태로 변경합니다.
     */
    public void transit(String location, String description) {
        updateStatus(DeliveryStatus.IN_TRANSIT, location, description);
    }

    /**
     * 배송 완료 처리합니다.
     */
    public void deliver(String location, String description) {
        updateStatus(DeliveryStatus.DELIVERED, location, description);
    }

    /**
     * 배송을 취소합니다.
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.DELIVERY_CANNOT_BE_CANCELLED);
        }
        this.status = DeliveryStatus.CANCELLED;
        addHistory(DeliveryStatus.CANCELLED, null, reason);
    }

    /**
     * 배송 이력을 추가합니다.
     */
    private void addHistory(DeliveryStatus status, String location, String description) {
        DeliveryHistory history = DeliveryHistory.builder()
                .delivery(this)
                .status(status)
                .location(location)
                .description(description)
                .build();

        this.histories.add(history);
    }

    /**
     * 운송장 번호를 생성합니다.
     * 형식: TRK-XXXXXXXXXXXX (12자리)
     */
    private static String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
