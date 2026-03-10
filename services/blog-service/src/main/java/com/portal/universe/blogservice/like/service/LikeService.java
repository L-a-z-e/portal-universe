package com.portal.universe.blogservice.like.service;

import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.event.BlogEventPublisher;
import com.portal.universe.blogservice.like.domain.Like;
import com.portal.universe.blogservice.like.dto.LikeStatusResponse;
import com.portal.universe.blogservice.like.dto.LikeToggleResponse;
import com.portal.universe.blogservice.like.dto.LikerResponse;
import com.portal.universe.blogservice.like.repository.LikeRepository;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private final MongoTemplate mongoTemplate;
    private final BlogEventPublisher eventPublisher;

    @Transactional
    public LikeToggleResponse toggleLike(String postId, String userId, String userName, String nickname) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        boolean liked;
        Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

        int increment;
        if (existingLike != null) {
            likeRepository.delete(existingLike);
            increment = -1;
            liked = false;
            log.info("Like removed: postId={}, userId={}", postId, userId);
        } else {
            Like newLike = Like.builder()
                    .postId(postId)
                    .userId(userId)
                    .userName(userName)
                    .nickname(nickname)
                    .build();
            Like savedLike = likeRepository.save(newLike);
            increment = 1;
            liked = true;
            log.info("Like added: postId={}, userId={}", postId, userId);

            // 자기 글에 좋아요한 경우 알림 발행하지 않음
            if (!userId.equals(post.getAuthorId())) {
                eventPublisher.publishPostLiked(PostLikedEvent.newBuilder()
                        .setLikeId(savedLike.getId())
                        .setPostId(postId)
                        .setPostTitle(post.getTitle())
                        .setAuthorId(post.getAuthorId())
                        .setLikerId(userId)
                        .setLikerName(nickname)
                        .setTimestamp(java.time.Instant.now())
                        .build());
            }
        }

        // atomic $inc — race condition 방지
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(postId)),
                new Update().inc("likeCount", increment),
                Post.class
        );

        return LikeToggleResponse.of(liked, post.getLikeCount() + increment);
    }

    public LikeStatusResponse getLikeStatus(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);

        return LikeStatusResponse.of(liked, post.getLikeCount());
    }

    public Page<LikerResponse> getLikers(String postId, Pageable pageable) {
        postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        return likeRepository.findByPostId(postId, pageable)
                .map(LikerResponse::from);
    }
}
