package com.portal.universe.blogservice.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BlogErrorCode implements ErrorCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "Post not found"),
    POST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B002", "You are not allowed to update this post"),
    POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B003", "You are not allowed to delete this post");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BlogErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
