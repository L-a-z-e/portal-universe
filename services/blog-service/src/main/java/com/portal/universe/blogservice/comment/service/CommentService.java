package com.portal.universe.blogservice.comment.service;

import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    /**
     * 댓글 생성
     */
    public CommentResponse createComment(CommentCreateRequest request, String authorId, String authorName) {
        Comment comment = Comment.builder()
                .postId(request.postId())
                .parentCommentId(request.parentCommentId())
                .authorId(authorId)
                .authorName(authorName)
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
    public CommentResponse updateComment(String commentId, CommentUpdateRequest request, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }

        comment.update(request.content());
        commentRepository.save(comment);
        return toResponse(comment);
    }

    /**
     * 댓글 삭제 (soft delete)
     */
    public void deleteComment(String commentId, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        comment.delete();
        commentRepository.save(comment);

        // Phase 3: 게시물의 댓글 수 감소
        updatePostCommentCount(comment.getPostId(), false);
    }

    /**
     * 게시물의 댓글 수 업데이트 (Phase 3: 트렌딩 점수 계산용)
     */
    private void updatePostCommentCount(String postId, boolean increment) {
        postRepository.findById(postId).ifPresent(post -> {
            if (increment) {
                post.incrementCommentCount();
            } else {
                post.decrementCommentCount();
            }
            postRepository.save(post);
            log.debug("Updated comment count for post {}: increment={}", postId, increment);
        });
    }

    /**
     * 특정 게시물의 모든 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(String postId) {
        List<Comment> comments = commentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);

        return comments.stream()
                .map(this::toResponse)
                .toList();
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
