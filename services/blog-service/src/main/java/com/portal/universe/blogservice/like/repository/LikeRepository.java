package com.portal.universe.blogservice.like.repository;

import com.portal.universe.blogservice.like.domain.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 좋아요 Repository
 * Spring Data MongoDB를 사용한 Like 엔티티 데이터 접근 계층
 */
public interface LikeRepository extends MongoRepository<Like, String> {

    /**
     * 특정 포스트에 대한 특정 사용자의 좋아요 조회
     * @param postId 포스트 ID
     * @param userId 사용자 ID
     * @return 좋아요 엔티티 (Optional)
     */
    Optional<Like> findByPostIdAndUserId(String postId, String userId);

    /**
     * 특정 포스트에 대한 좋아요 존재 여부 확인
     * @param postId 포스트 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean existsByPostIdAndUserId(String postId, String userId);

    /**
     * 특정 포스트의 모든 좋아요 조회 (페이징)
     * @param postId 포스트 ID
     * @param pageable 페이징 정보
     * @return 좋아요 목록 (페이징)
     */
    Page<Like> findByPostId(String postId, Pageable pageable);

    /**
     * 특정 포스트의 좋아요 개수 조회
     * @param postId 포스트 ID
     * @return 좋아요 개수
     */
    long countByPostId(String postId);
}
