package com.portal.universe.blogservice.comment.service;

import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.exception.CommentNotFoundException;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentResponse createComment(CommentCreateRequest request, String authorId, String authorName) {
        Comment comment = Comment.builder()
                .postId(request.getPostId())
                .parentCommentId(request.getParentCommentId())
                .authorId(authorId)
                .authorName(authorName)
                .content(request.getContent())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        commentRepository.save(comment);
        return toResponse(comment);
    }

    public CommentResponse updateComment(String commentId, CommentUpdateRequest request, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getAuthorId().equals(authorId))
            throw new IllegalStateException("작성자만 수정할 수 있습니다.");

        comment.update(request.getContent());
        commentRepository.save(comment);
        return toResponse(comment);
    }

    public void deleteComment(String commentId, String authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getAuthorId().equals(authorId))
            throw new IllegalStateException("작성자만 삭제할 수 있습니다.");

        comment.delete();
        commentRepository.save(comment);
    }

    public List<CommentResponse> getCommentsByPostId(String postId) {
        List<Comment> comments = commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
        return comments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private CommentResponse toResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setAuthorId(comment.getAuthorId());
        response.setAuthorName(comment.getAuthorName());
        response.setContent(comment.getContent());
        response.setParentCommentId(comment.getParentCommentId());
        response.setLikeCount(comment.getLikeCount());
        response.setIsDeleted(comment.getIsDeleted());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }
}
