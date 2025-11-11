package com.portal.universe.blogservice.comment.repository;

import com.portal.universe.blogservice.comment.domain.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(String postId);

    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(String parentCommentId);

    List<Comment> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(String authorId);
}