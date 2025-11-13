package com.portal.universe.blogservice.file.exception;

/**
 * 파일 업로드 관련 커스텀 예외
 */
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}