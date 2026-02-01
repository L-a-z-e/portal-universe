package com.portal.universe.shoppingservice.product.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 정보를 나타내는 JPA 엔티티 클래스입니다.
 */
@Entity
@Table(name = "products") // 데이터베이스의 'products' 테이블과 매핑
@EntityListeners(AuditingEntityListener.class)
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

    /**
     * 상품 이미지 URL
     */
    private String imageUrl;

    /**
     * 상품 카테고리
     */
    private String category;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Product(String name, String description, Double price, Integer stock, String imageUrl, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    /**
     * 상품 정보를 수정합니다.
     * @param name 새로운 상품명
     * @param description 새로운 상품 설명
     * @param price 새로운 가격
     * @param stock 새로운 재고 수량
     * @param imageUrl 새로운 이미지 URL
     * @param category 새로운 카테고리
     */
    public void update(String name, String description, Double price, Integer stock, String imageUrl, String category) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock = stock;
            this.imageUrl = imageUrl;
            this.category = category;
    }
}
