package com.portal.universe.blogservice.dto;

/**
 * 새로운 게시물 생성을 요청할 때 사용하는 DTO(Data Transfer Object)입니다.
 * Java 14의 record를 사용하여 불변 객체로 정의되었습니다.
 *
 * @param title 게시물 제목
 * @param content 게시물 내용
 * @param productId 연관된 상품 ID
 */
public record PostCreateRequest(
        String title,
        String content,
        String productId
) {}