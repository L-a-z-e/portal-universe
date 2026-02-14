package com.portal.universe.shoppingsellerservice.seller.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sellers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "representative_name", length = 50)
    private String representativeName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_account", length = 30)
    private String bankAccount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Seller(Long userId, String businessName, String businessNumber,
                  String representativeName, String phone, String email,
                  String bankName, String bankAccount, BigDecimal commissionRate) {
        this.userId = userId;
        this.businessName = businessName;
        this.businessNumber = businessNumber;
        this.representativeName = representativeName;
        this.phone = phone;
        this.email = email;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.commissionRate = commissionRate != null ? commissionRate : new BigDecimal("10.00");
        this.status = SellerStatus.PENDING;
    }

    public void approve() {
        this.status = SellerStatus.ACTIVE;
    }

    public void suspend() {
        this.status = SellerStatus.SUSPENDED;
    }

    public void withdraw() {
        this.status = SellerStatus.WITHDRAWN;
    }

    public void update(String businessName, String phone, String email,
                       String bankName, String bankAccount) {
        this.businessName = businessName;
        this.phone = phone;
        this.email = email;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
    }

    public boolean isActive() {
        return this.status == SellerStatus.ACTIVE;
    }
}
