package com.portal.universe.shoppingservice.order.domain;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 주문을 나타내는 JPA 엔티티입니다.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 번호 (UUID 기반, 읽기 쉬운 형태)
     * 형식: ORD-YYYYMMDD-XXXXXXXX
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    /**
     * 주문자 ID
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * 주문 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /**
     * 총 주문 금액
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 배송 주소
     */
    @Embedded
    private Address shippingAddress;

    /**
     * 주문 항목 목록
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * 취소 사유
     */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    /**
     * 취소 일시
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Order(String userId, Address shippingAddress) {
        this.orderNumber = generateOrderNumber();
        this.userId = userId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
        this.shippingAddress = shippingAddress;
    }

    /**
     * 주문에 항목을 추가합니다.
     */
    public OrderItem addItem(Long productId, String productName, BigDecimal price, int quantity) {
        OrderItem orderItem = OrderItem.builder()
                .order(this)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();

        this.items.add(orderItem);
        recalculateTotalAmount();
        return orderItem;
    }

    /**
     * 주문 상태를 확정(CONFIRMED)으로 변경합니다.
     */
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 주문 상태를 결제 완료(PAID)로 변경합니다.
     */
    public void markAsPaid() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.PAID;
    }

    /**
     * 주문 상태를 배송 중(SHIPPING)으로 변경합니다.
     */
    public void ship() {
        if (this.status != OrderStatus.PAID) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.SHIPPING;
    }

    /**
     * 주문 상태를 배송 완료(DELIVERED)로 변경합니다.
     */
    public void deliver() {
        if (this.status != OrderStatus.SHIPPING) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.DELIVERED;
    }

    /**
     * 주문을 취소합니다.
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 주문을 환불 처리합니다.
     */
    public void refund() {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.REFUNDED;
    }

    /**
     * 총 주문 금액을 재계산합니다.
     */
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 총 상품 수량을 반환합니다.
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    /**
     * 주문번호를 생성합니다.
     * 형식: ORD-YYYYMMDD-XXXXXXXX (UUID의 앞 8자리)
     */
    private static String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuidSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + datePrefix + "-" + uuidSuffix;
    }
}
