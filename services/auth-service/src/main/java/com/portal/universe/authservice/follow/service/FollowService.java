package com.portal.universe.authservice.follow.service;

import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.follow.domain.Follow;
import com.portal.universe.authservice.follow.dto.*;
import com.portal.universe.authservice.follow.repository.FollowRepository;
import com.portal.universe.authservice.repository.UserRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 팔로우 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 팔로우 토글 (팔로우/언팔로우)
     * 이미 팔로우 중이면 언팔로우, 아니면 팔로우
     */
    @Transactional
    public FollowResponse toggleFollow(Long currentUserId, String targetUsername) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        // 자기 자신 팔로우 방지
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new CustomBusinessException(AuthErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        boolean isFollowing;
        if (followRepository.existsByFollowerAndFollowing(currentUser, targetUser)) {
            // 이미 팔로우 중이면 언팔로우
            Follow follow = followRepository.findByFollowerAndFollowing(currentUser, targetUser)
                    .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.NOT_FOLLOWING));
            followRepository.delete(follow);
            isFollowing = false;
        } else {
            // 팔로우
            Follow follow = new Follow(currentUser, targetUser);
            followRepository.save(follow);
            isFollowing = true;
        }

        // 기존 헬퍼 메서드 재사용 (DRY 원칙)
        return new FollowResponse(
                isFollowing,
                getFollowerCount(targetUser),
                getFollowingCount(targetUser)
        );
    }

    /**
     * 팔로워 목록 조회
     */
    public FollowListResponse getFollowers(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followers = followRepository.findFollowersByUser(user, pageable);
        Page<FollowUserResponse> followUserResponses = followers.map(FollowUserResponse::from);

        return FollowListResponse.from(followUserResponses);
    }

    /**
     * 팔로잉 목록 조회
     */
    public FollowListResponse getFollowings(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followings = followRepository.findFollowingsByUser(user, pageable);
        Page<FollowUserResponse> followUserResponses = followings.map(FollowUserResponse::from);

        return FollowListResponse.from(followUserResponses);
    }

    /**
     * 내가 팔로우하는 사용자들의 UUID 목록 조회
     * blog-service 피드 API에서 사용
     */
    public FollowingIdsResponse getMyFollowingIds(Long currentUserId) {
        List<String> followingIds = followRepository.findFollowingUuidsByFollowerId(currentUserId);
        return new FollowingIdsResponse(followingIds);
    }

    /**
     * 팔로우 상태 확인
     */
    public FollowStatusResponse getFollowStatus(Long currentUserId, String targetUsername) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        boolean isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, targetUser);
        return new FollowStatusResponse(isFollowing);
    }

    /**
     * 특정 사용자의 팔로워 수 조회
     */
    public int getFollowerCount(User user) {
        return (int) followRepository.countByFollowing(user);
    }

    /**
     * 특정 사용자의 팔로잉 수 조회
     */
    public int getFollowingCount(User user) {
        return (int) followRepository.countByFollower(user);
    }

    /**
     * username으로 팔로워/팔로잉 카운트 조회
     */
    public int[] getFollowCounts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        int followerCount = getFollowerCount(user);
        int followingCount = getFollowingCount(user);

        return new int[]{followerCount, followingCount};
    }
}
