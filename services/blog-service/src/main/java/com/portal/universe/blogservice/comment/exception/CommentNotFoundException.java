package com.portal.universe.blogservice.comment.exception;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(String commentId) {
        super("Comment not found: " + commentId);
    }
}