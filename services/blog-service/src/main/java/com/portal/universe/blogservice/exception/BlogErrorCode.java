package com.portal.universe.blogservice.exception;

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
    LIKE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B022", "Like operation failed");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BlogErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}