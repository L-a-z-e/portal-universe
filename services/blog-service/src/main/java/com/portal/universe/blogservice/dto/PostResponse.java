package com.portal.universe.blogservice.dto;

import java.time.LocalDateTime;

/**
 * 게시물 조회 결과를 반환할 때 사용하는 DTO(Data Transfer Object)입니다.
 * Java 14의 record를 사용하여 불변 객체로 정의되었습니다.
 *
 * @param id 게시물 ID
 * @param title 게시물 제목
 * @param content 게시물 내용
 * @param authorId 작성자 ID
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 * @param productId 연관된 상품 ID
 */
public record PostResponse(
        String id,
        String title,
        String content,
        String authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String productId
) {
}