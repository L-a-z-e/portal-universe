package com.portal.universe.authservice.user.domain;

import com.portal.universe.authservice.oauth2.domain.SocialAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 핵심 정보를 담는 엔티티입니다. (Core Identity)
 * 식별, 인증, 상태 관리에 집중합니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // 외부 노출용 식별자 (DataInitializer에서 고정 UUID 사전 할당 가능)
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column // 소셜 로그인의 경우 null 가능
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime lastLoginAt;

    private LocalDateTime passwordChangedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 1:1 관계 - UserProfile
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private UserProfile profile;

    // 1:N 관계 - SocialAccount
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.status = UserStatus.ACTIVE;
    }

    @PrePersist
    private void generateUuidIfNeeded() {
        if (this.uuid == null) {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
    }

    public void assignUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    /**
     * 비밀번호를 변경합니다.
     * @param encodedPassword 암호화된 새 비밀번호
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴를 요청합니다. (Soft Delete)
     * 상태를 WITHDRAWAL_PENDING으로 변경합니다.
     */
    public void markForWithdrawal() {
        this.status = UserStatus.WITHDRAWAL_PENDING;
    }

    /**
     * 소셜 로그인 사용자인지 확인합니다.
     * 비밀번호가 null이고 소셜 계정이 연결되어 있으면 소셜 사용자입니다.
     */
    public boolean isSocialUser() {
        return this.password == null && !this.socialAccounts.isEmpty();
    }
}