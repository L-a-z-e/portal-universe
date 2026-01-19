package com.portal.universe.shoppingservice.product.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 정보를 나타내는 JPA 엔티티 클래스입니다.
 */
@Entity
@Table(name = "products") // 데이터베이스의 'products' 테이블과 매핑
@Getter
@NoArgsConstructor // JPA 프록시 생성을 위한 기본 생성자
public class Product {

    /**
     * 상품의 고유 ID (Auto Increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품명
     */
    @Column(nullable = false)
    private String name;

    /**
     * 상품 설명
     */
    private String description;

    /**
     * 상품 가격
     */
    @Column(nullable = false)
    private Double price;

    /**
     * 상품 재고 수량
     */
    @Column(nullable = false)
    private Integer stock;

    @Builder
    public Product(String name, String description, Double price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    /**
     * 상품 정보를 수정합니다.
     * @param name 새로운 상품명
     * @param description 새로운 상품 설명
     * @param price 새로운 가격
     * @param stock 새로운 재고 수량
     */
    public void update(String name, String description, Double price, Integer stock) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock = stock;
    }
}
