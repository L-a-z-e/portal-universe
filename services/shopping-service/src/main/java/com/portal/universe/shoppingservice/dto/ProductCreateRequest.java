package com.portal.universe.shoppingservice.dto;

/**
 * 새로운 상품 생성을 요청할 때 사용하는 DTO입니다.
 *
 * @param name 상품명
 * @param description 상품 설명
 * @param price 가격
 * @param stock 재고
 */
public record ProductCreateRequest(String name, String description, Double price, Integer stock) {
}