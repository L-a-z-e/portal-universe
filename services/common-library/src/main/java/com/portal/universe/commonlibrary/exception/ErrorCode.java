package com.portal.universe.commonlibrary.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}