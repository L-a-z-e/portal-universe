package com.portal.universe.shoppingservice.dto;

/**
 * Blog 서비스로부터 Feign을 통해 받아온 게시물(리뷰) 정보를 담는 DTO입니다.
 *
 * @param id 게시물 ID
 * @param title 제목
 * @param content 내용
 * @param authorId 작성자 ID
 * @param productId 연관된 상품 ID
 */
public record BlogResponse(
        String id,
        String title,
        String content,
        String authorId,
        String productId
) {}