package com.portal.universe.blogservice.comment.service;

import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.event.BlogEventPublisher;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;
    private final BlogEventPublisher eventPublisher;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, String authorId, String authorUsername, String authorNickname) {
        // 게시물 조회 (알림 발행을 위해)
        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        String decodedAuthorNickname = decodeHeaderValue(authorNickname);
        Comment comment = Comment.builder()
                .postId(request.postId())
                .parentCommentId(request.parentCommentId())
                .authorId(authorId)
                .authorUsername(authorUsername)
                .authorNickname(decodedAuthorNickname)
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Phase 3: 게시물의 댓글 수 증가
        updatePostCommentCount(request.postId(), true);

        // 자기 글에 댓글을 단 경우 알림 발행하지 않음
        if (!authorId.equals(post.getAuthorId())) {
            eventPublisher.publishCommentCreated(CommentCreatedEvent.newBuilder()
                    .setCommentId(savedComment.getId())
                    .setPostId(request.postId())
                    .setPostTitle(post.getTitle())
                    .setAuthorId(post.getAuthorId())
                    .setCommenterId(authorId)
                    .setCommenterName(decodedAuthorNickname)
                    .setContent(request.content())
                    .setTimestamp(java.time.Instant.now())
                    .build());
        }

        return toResponse(savedComment);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(String commentId, CommentUpdateRequest request, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthorId().equals(authorId)
                && !SecurityUtils.isServiceAdmin("BLOG")) {
            throw new CustomBusinessException(BlogErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }

        comment.update(request.content());
        commentRepository.save(comment);
        return toResponse(comment);
    }

    /**
     * 댓글 삭제 (soft delete)
     */
    @Transactional
    public void deleteComment(String commentId, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthorId().equals(authorId)
                && !SecurityUtils.isServiceAdmin("BLOG")) {
            throw new CustomBusinessException(BlogErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        comment.delete();
        commentRepository.save(comment);

        // Phase 3: 게시물의 댓글 수 감소
        updatePostCommentCount(comment.getPostId(), false);
    }

    /**
     * 게시물의 댓글 수 업데이트 (atomic $inc — race condition 방지)
     */
    private void updatePostCommentCount(String postId, boolean increment) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(postId)),
                new Update().inc("commentCount", increment ? 1 : -1),
                Post.class
        );
        log.debug("Updated comment count for post {}: increment={}", postId, increment);
    }

    /**
     * 특정 게시물의 모든 댓글 조회
     */
    public List<CommentResponse> getCommentsByPostId(String postId) {
        List<Comment> comments = commentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);

        return comments.stream()
                .map(this::toResponse)
                .toList();
    }

    private String decodeHeaderValue(String value) {
        if (value == null) return null;
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Entity → DTO 변환
     */
    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getAuthorUsername(),
                comment.getAuthorNickname(),
                comment.getContent(),
                comment.getParentCommentId(),
                comment.getLikeCount(),
                comment.getIsDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
