package com.portal.universe.blogservice.like.service;

import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.like.domain.Like;
import com.portal.universe.blogservice.like.dto.LikeStatusResponse;
import com.portal.universe.blogservice.like.dto.LikeToggleResponse;
import com.portal.universe.blogservice.like.dto.LikerResponse;
import com.portal.universe.blogservice.like.repository.LikeRepository;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 비즈니스 로직 서비스
 * 좋아요 추가/취소 및 Post의 likeCount 동기화 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    /**
     * 좋아요 토글 (추가/취소)
     * 이미 좋아요가 있으면 취소, 없으면 추가
     * @param postId 포스트 ID
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     * @return 좋아요 토글 결과 (liked 상태, 총 좋아요 수)
     */
    @Transactional
    public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
        // 1. Post 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 2. 기존 좋아요 확인
        boolean liked;
        Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

        if (existingLike != null) {
            // 좋아요 취소
            likeRepository.delete(existingLike);
            post.decrementLikeCount();
            liked = false;
            log.info("Like removed: postId={}, userId={}", postId, userId);
        } else {
            // 좋아요 추가
            Like newLike = Like.builder()
                    .postId(postId)
                    .userId(userId)
                    .userName(userName)
                    .build();
            likeRepository.save(newLike);
            post.incrementLikeCount();
            liked = true;
            log.info("Like added: postId={}, userId={}", postId, userId);
        }

        // 3. Post 저장 (likeCount 반영)
        postRepository.save(post);

        return LikeToggleResponse.of(liked, post.getLikeCount());
    }

    /**
     * 좋아요 상태 확인
     * 현재 사용자가 해당 포스트를 좋아요했는지 여부와 전체 좋아요 수 반환
     * @param postId 포스트 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 (liked 여부, 총 좋아요 수)
     */
    public LikeStatusResponse getLikeStatus(String postId, String userId) {
        // 1. Post 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 2. 좋아요 여부 확인
        boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);

        return LikeStatusResponse.of(liked, post.getLikeCount());
    }

    /**
     * 좋아요한 사용자 목록 조회 (페이징)
     * @param postId 포스트 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 사용자 목록 (페이징)
     */
    public Page<LikerResponse> getLikers(String postId, Pageable pageable) {
        // Post 존재 여부 확인
        postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        return likeRepository.findByPostId(postId, pageable)
                .map(LikerResponse::from);
    }
}
