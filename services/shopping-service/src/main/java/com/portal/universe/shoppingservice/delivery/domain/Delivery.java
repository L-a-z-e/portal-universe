package com.portal.universe.shoppingservice.delivery.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.domain.Address;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
        @Index(name = "idx_delivery_status", columnList = "status"),
        @Index(name = "idx_delivery_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 30)
    private String trackingNumber;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId; // 소유권 검증용 denormalization

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "carrier", length = 50)
    private String carrier;

    @Embedded
    private Address shippingAddress;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<DeliveryHistory> histories = new ArrayList<>();

    @Builder
    public Delivery(Long orderId, String orderNumber, String userId, Address shippingAddress, String carrier) {
        this.trackingNumber = generateTrackingNumber();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.status = DeliveryStatus.PREPARING;
        this.shippingAddress = shippingAddress;
        this.carrier = carrier != null ? carrier : "기본택배";
        this.estimatedDeliveryDate = LocalDate.now().plusDays(3); // 기본 3일 후
    }

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

    public void ship(String location, String description) {
        updateStatus(DeliveryStatus.SHIPPED, location, description);
    }

    public void transit(String location, String description) {
        updateStatus(DeliveryStatus.IN_TRANSIT, location, description);
    }

    public void deliver(String location, String description) {
        updateStatus(DeliveryStatus.DELIVERED, location, description);
    }

    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.DELIVERY_CANNOT_BE_CANCELLED);
        }
        this.status = DeliveryStatus.CANCELLED;
        addHistory(DeliveryStatus.CANCELLED, null, reason);
    }

    private void addHistory(DeliveryStatus status, String location, String description) {
        DeliveryHistory history = DeliveryHistory.builder()
                .delivery(this)
                .status(status)
                .location(location)
                .description(description)
                .build();

        this.histories.add(history);
    }

    // 형식: TRK-XXXXXXXXXXXX (12자리)
    private static String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
