package com.portal.universe.driveservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DriveErrorCode implements ErrorCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "File not found"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D002", "File upload failed"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "D003", "File size exceeds limit"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "D004", "File type not allowed"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D005", "File delete failed"),

    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "D010", "Folder not found"),
    FOLDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "D011", "Folder already exists"),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "D020", "Access denied to this resource"),
    STORAGE_QUOTA_EXCEEDED(HttpStatus.BAD_REQUEST, "D030", "Storage quota exceeded");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DriveErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
