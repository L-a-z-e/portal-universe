package com.portal.universe.shoppingservice.delivery.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 배송 이력을 나타내는 JPA 엔티티입니다.
 */
@Entity
@Table(name = "delivery_histories", indexes = {
        @Index(name = "idx_delivery_history_delivery_id", columnList = "delivery_id"),
        @Index(name = "idx_delivery_history_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 배송
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    /**
     * 배송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    /**
     * 위치 정보
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * 상세 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public DeliveryHistory(Delivery delivery, DeliveryStatus status, String location, String description) {
        this.delivery = delivery;
        this.status = status;
        this.location = location;
        this.description = description;
    }
}
