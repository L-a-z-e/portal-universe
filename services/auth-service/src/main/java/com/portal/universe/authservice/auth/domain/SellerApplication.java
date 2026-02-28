package com.portal.universe.authservice.auth.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "seller_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "business_number", length = 50)
    private String businessNumber;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerApplicationStatus status = SellerApplicationStatus.PENDING;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Builder
    public SellerApplication(String userId, String businessName,
                             String businessNumber, String reason) {
        this.userId = userId;
        this.businessName = businessName;
        this.businessNumber = businessNumber;
        this.reason = reason;
        this.status = SellerApplicationStatus.PENDING;
    }

    public void approve(String reviewedBy, String comment) {
        this.status = SellerApplicationStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
    }

    public void reject(String reviewedBy, String comment) {
        this.status = SellerApplicationStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
    }

    public boolean isPending() {
        return this.status == SellerApplicationStatus.PENDING;
    }
}
