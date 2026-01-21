package com.portal.universe.authservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 부가 정보(프로필)를 담는 엔티티입니다.
 * User 테이블과 1:1 관계를 가집니다.
 */
@Entity
@Table(name = "user_profiles",
        indexes = @Index(name = "idx_username", columnList = "username", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    private Long userId;

    @MapsId // User 엔티티의 PK를 이 엔티티의 PK이자 FK로 사용
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 50)
    private String realName;

    @Column(unique = true, length = 20)
    private String username;

    @Column(length = 200)
    private String bio;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String profileImageUrl;

    @Column(length = 255)
    private String website;

    @Column(nullable = false)
    private boolean marketingAgree;

    public UserProfile(User user, String nickname, String realName, boolean marketingAgree) {
        this.user = user;
        this.nickname = nickname;
        this.realName = realName;
        this.marketingAgree = marketingAgree;
    }

    public UserProfile(User user, String nickname, String profileImageUrl) {
        this.user = user;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.marketingAgree = false;
    }

// ========== Update Methods ==========

    public void setUsername(String username) {
        this.username = username;
    }

    public void updateNickname(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }

    public void updateRealName(String realName) {
        this.realName = realName;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updateWebsite(String website) {
        this.website = website;
    }

    public void updateMarketingAgree(boolean marketingAgree) {
        this.marketingAgree = marketingAgree;
    }

    public void updateProfile(String nickname, String bio, String profileImageUrl, String website) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.website = website;
    }
}
