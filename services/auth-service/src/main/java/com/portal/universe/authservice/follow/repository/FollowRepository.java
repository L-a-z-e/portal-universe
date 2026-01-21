package com.portal.universe.authservice.follow.repository;

import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.follow.domain.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Follow 엔티티에 대한 데이터 접근을 처리하는 Repository입니다.
 */
public interface FollowRepository extends JpaRepository<Follow, Long> {

    /**
     * 팔로우 관계가 존재하는지 확인합니다.
     */
    boolean existsByFollowerAndFollowing(User follower, User following);

    /**
     * 특정 팔로우 관계를 조회합니다.
     */
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    /**
     * 특정 사용자의 팔로워 목록을 조회합니다.
     */
    @Query("SELECT f.follower FROM Follow f WHERE f.following = :user ORDER BY f.createdAt DESC")
    Page<User> findFollowersByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 사용자가 팔로우하는 목록을 조회합니다.
     */
    @Query("SELECT f.following FROM Follow f WHERE f.follower = :user ORDER BY f.createdAt DESC")
    Page<User> findFollowingsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 사용자의 팔로워 수를 조회합니다.
     */
    long countByFollowing(User user);

    /**
     * 특정 사용자가 팔로우하는 수를 조회합니다.
     */
    long countByFollower(User user);

    /**
     * 특정 사용자가 팔로우하는 사용자들의 UUID 목록을 조회합니다.
     * blog-service 피드 API에서 사용합니다.
     */
    @Query("SELECT f.following.uuid FROM Follow f WHERE f.follower.id = :userId")
    List<String> findFollowingUuidsByFollowerId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 팔로우하는 사용자들의 ID 목록을 조회합니다.
     */
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
    List<Long> findFollowingIdsByFollowerId(@Param("userId") Long userId);

    /**
     * 팔로워 또는 팔로잉 사용자로 검색하여 관계 삭제 (회원 탈퇴 시)
     */
    void deleteByFollowerOrFollowing(User follower, User following);
}
