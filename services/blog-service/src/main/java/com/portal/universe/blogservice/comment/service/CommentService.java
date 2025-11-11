package com.portal.universe.blogservice.comment.service;

import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.exception.CommentNotFoundException;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

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
        return toResponse(comment);
    }

    /**
     * 댓글 수정
     */
    public CommentResponse updateComment(String commentId, CommentUpdateRequest request, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 수정할 수 있습니다.");
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
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 삭제할 수 있습니다.");
        }

        comment.delete();
        commentRepository.save(comment);
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
