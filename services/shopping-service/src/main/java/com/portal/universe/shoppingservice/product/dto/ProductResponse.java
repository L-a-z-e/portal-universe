package com.portal.universe.shoppingservice.product.dto;

/**
 * 상품 정보 조회 결과를 반환할 때 사용하는 DTO입니다.
 *
 * @param id 상품 ID
 * @param name 상품명
 * @param description 상품 설명
 * @param price 가격
 * @param stock 재고
 */
public record ProductResponse(Long id, String name, String description, Double price, Integer stock, String imageUrl, String category) {
}
