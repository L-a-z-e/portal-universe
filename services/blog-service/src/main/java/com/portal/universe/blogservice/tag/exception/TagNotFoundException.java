package com.portal.universe.blogservice.tag.exception;

/**
 * 태그를 찾을 수 없을 때 발생하는 예외
 */
public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String tagName) {
        super("Tag not found: " + tagName);
    }

    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}