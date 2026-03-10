package com.portal.universe.blogservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Blog 서비스 오류 코드 정의.
 */
@Getter
public enum BlogErrorCode implements ErrorCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "Post not found"),
    POST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B002", "You are not allowed to update this post"),
    POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B003", "You are not allowed to delete this post"),
    POST_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "B004", "Post is not published yet"),

    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "B020", "Like not found"),
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "B021", "Like already exists"),
    LIKE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B022", "Like operation failed"),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B030", "Comment not found"),
    COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B031", "You are not allowed to update this comment"),
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B032", "You are not allowed to delete this comment"),

    SERIES_NOT_FOUND(HttpStatus.NOT_FOUND, "B040", "Series not found"),
    SERIES_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B041", "You are not allowed to update this series"),
    SERIES_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B042", "You are not allowed to delete this series"),
    SERIES_ADD_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "B043", "You are not allowed to add posts to this series"),
    SERIES_REMOVE_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "B044", "You are not allowed to remove posts from this series"),
    SERIES_REORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "B045", "You are not allowed to reorder posts in this series"),
    SERIES_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "B046", "Series was modified by another request. Please retry."),

    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "B050", "Tag not found"),
    TAG_ALREADY_EXISTS(HttpStatus.CONFLICT, "B051", "Tag already exists"),

    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B060", "File upload failed"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "B061", "File is empty"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "B062", "File size exceeds limit"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "B063", "File type not allowed"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B064", "File delete failed"),
    INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "B065", "Invalid file URL format");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BlogErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}