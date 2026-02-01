package com.portal.universe.blogservice.comment.service;

import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.domain.Post;
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
    private final MongoTemplate mongoTemplate;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, String authorId, String authorName) {
        Comment comment = Comment.builder()
                .postId(request.postId())
                .parentCommentId(request.parentCommentId())
                .authorId(authorId)
                .authorName(decodeHeaderValue(authorName))
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);

        // Phase 3: 게시물의 댓글 수 증가
        updatePostCommentCount(request.postId(), true);

        return toResponse(comment);
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
                comment.getAuthorName(),
                comment.getContent(),
                comment.getParentCommentId(),
                comment.getLikeCount(),
                comment.getIsDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
