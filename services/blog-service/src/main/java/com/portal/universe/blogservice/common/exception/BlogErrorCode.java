package com.portal.universe.blogservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 블로그(Blog) 서비스에서 발생하는 비즈니스 예외에 대한 오류 코드를 정의하는 열거형 클래스입니다.
 * 각 오류 코드는 HTTP 상태, 고유 코드, 그리고 메시지를 포함합니다.
 */
@Getter
public enum BlogErrorCode implements ErrorCode {

    /**
     * 요청한 ID에 해당하는 게시물이 존재하지 않을 경우 발생합니다.
     */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "Post not found"),

    /**
     * 게시물 작성자가 아닌 다른 사용자가 게시물 수정을 시도할 경우 발생합니다.
     */
    POST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B002", "You are not allowed to update this post"),

    /**
     * 게시물 작성자가 아닌 다른 사용자가 게시물 삭제를 시도할 경우 발생합니다.
     */
    POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B003", "You are not allowed to delete this post"),

    /**
     * 게시물이 아직 발행되지 않은 경우 발생합니다.
     */
    POST_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "B004", "Post is not published yet"),

    /**
     * 요청한 ID에 해당하는 좋아요가 존재하지 않을 경우 발생합니다.
     */
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "B020", "Like not found"),

    /**
     * 좋아요가 이미 존재하는 경우 발생합니다.
     */
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "B021", "Like already exists"),

    /**
     * 좋아요 작업 중 예기치 않은 오류가 발생한 경우입니다.
     */
    LIKE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B022", "Like operation failed"),

    // ========================================
    // Comment Errors (B030 ~ B039)
    // ========================================

    /**
     * 댓글을 찾을 수 없는 경우 발생합니다.
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B030", "Comment not found"),

    /**
     * 댓글 작성자가 아닌 다른 사용자가 수정을 시도할 경우 발생합니다.
     */
    COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B031", "You are not allowed to update this comment"),

    /**
     * 댓글 작성자가 아닌 다른 사용자가 삭제를 시도할 경우 발생합니다.
     */
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B032", "You are not allowed to delete this comment"),

    // ========================================
    // Series Errors (B040 ~ B049)
    // ========================================

    /**
     * 시리즈를 찾을 수 없는 경우 발생합니다.
     */
    SERIES_NOT_FOUND(HttpStatus.NOT_FOUND, "B040", "Series not found"),

    /**
     * 시리즈 작성자가 아닌 다른 사용자가 수정을 시도할 경우 발생합니다.
     */
    SERIES_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B041", "You are not allowed to update this series"),

    /**
     * 시리즈 작성자가 아닌 다른 사용자가 삭제를 시도할 경우 발생합니다.
     */
    SERIES_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B042", "You are not allowed to delete this series"),

    /**
     * 시리즈 작성자가 아닌 다른 사용자가 포스트를 추가하려고 할 경우 발생합니다.
     */
    SERIES_ADD_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "B043", "You are not allowed to add posts to this series"),

    /**
     * 시리즈 작성자가 아닌 다른 사용자가 포스트를 제거하려고 할 경우 발생합니다.
     */
    SERIES_REMOVE_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "B044", "You are not allowed to remove posts from this series"),

    /**
     * 시리즈 작성자가 아닌 다른 사용자가 순서를 변경하려고 할 경우 발생합니다.
     */
    SERIES_REORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "B045", "You are not allowed to reorder posts in this series"),

    // ========================================
    // Tag Errors (B050 ~ B059)
    // ========================================

    /**
     * 태그를 찾을 수 없는 경우 발생합니다.
     */
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "B050", "Tag not found"),

    /**
     * 태그가 이미 존재하는 경우 발생합니다.
     */
    TAG_ALREADY_EXISTS(HttpStatus.CONFLICT, "B051", "Tag already exists"),

    // ========================================
    // File Errors (B060 ~ B069)
    // ========================================

    /**
     * 파일 업로드에 실패한 경우 발생합니다.
     */
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B060", "File upload failed"),

    /**
     * 파일이 비어있는 경우 발생합니다.
     */
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "B061", "File is empty"),

    /**
     * 파일 크기가 제한을 초과한 경우 발생합니다.
     */
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "B062", "File size exceeds limit"),

    /**
     * 허용되지 않는 파일 형식인 경우 발생합니다.
     */
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "B063", "File type not allowed"),

    /**
     * 파일 삭제에 실패한 경우 발생합니다.
     */
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B064", "File delete failed"),

    /**
     * 잘못된 파일 URL 형식인 경우 발생합니다.
     */
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