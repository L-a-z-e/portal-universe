package com.portal.universe.shoppingservice.product.dto;

import com.portal.universe.shoppingservice.feign.dto.BlogResponse;

import java.util.List;

/**
 * 상품 정보와 해당 상품의 리뷰 목록을 함께 반환할 때 사용하는 DTO입니다.
 *
 * @param id 상품 ID
 * @param name 상품명
 * @param description 상품 설명
 * @param price 가격
 * @param stock 재고
 * @param reviews 리뷰 목록 (Blog 서비스로부터 받은 정보)
 */
public record ProductWithReviewsResponse(
        Long id,
        String name,
        String description,
        Double price,
        Integer stock,
        List<BlogResponse> reviews
) {}
