package com.portal.universe.authservice.follow.domain;

import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 간의 팔로우 관계를 나타내는 엔티티입니다.
 * follower가 following을 팔로우합니다.
 */
@Entity
@Table(name = "follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_follow_relationship",
                columnNames = {"follower_id", "following_id"}
        ),
        indexes = {
                @Index(name = "idx_follower_id", columnList = "follower_id"),
                @Index(name = "idx_following_id", columnList = "following_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    public Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
    }
}
