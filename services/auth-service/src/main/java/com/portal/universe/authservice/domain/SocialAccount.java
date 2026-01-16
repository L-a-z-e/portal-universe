package com.portal.universe.authservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 연동 정보를 담는 엔티티입니다.
 * User 테이블과 N:1 관계를 가집니다.
 */
@Entity
@Table(name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId; // 공급자 측 식별자 (sub)

    @Column(name = "access_token")
    private String accessToken; // 선택적 저장 (암호화 권장)

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime connectedAt;

    public SocialAccount(User user, SocialProvider provider, String providerId) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
    }
}
