package com.portal.universe.authservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
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

    // 외부 노출용 식별자 (UUID v7 권장되나 편의상 기본 UUID 전략 사용)
    @UuidGenerator
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column // 소셜 로그인의 경우 null 가능
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime lastLoginAt;

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

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}