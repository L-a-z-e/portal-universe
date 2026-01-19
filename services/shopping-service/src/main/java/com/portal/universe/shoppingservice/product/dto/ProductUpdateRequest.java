package com.portal.universe.shoppingservice.product.dto;

/**
 * 상품 정보 수정을 요청할 때 사용하는 DTO입니다.
 *
 * @param name 수정할 상품명
 * @param description 수정할 상품 설명
 * @param price 수정할 가격
 * @param stock 수정할 재고
 */
public record ProductUpdateRequest(String name, String description, Double price, Integer stock) {
}
