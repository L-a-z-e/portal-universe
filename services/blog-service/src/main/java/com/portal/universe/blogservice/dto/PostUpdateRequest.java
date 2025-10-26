package com.portal.universe.blogservice.dto;

/**
 * 게시물 수정을 요청할 때 사용하는 DTO(Data Transfer Object)입니다.
 * Java 14의 record를 사용하여 불변 객체로 정의되었습니다.
 *
 * @param title 수정할 게시물 제목
 * @param content 수정할 게시물 내용
 */
public record PostUpdateRequest(
        String title,
        String content
) {
}