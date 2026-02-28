package com.portal.universe.shoppingsellerservice.product.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 19, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;

    @Column(nullable = false)
    private Boolean featured = false;

    @Builder
    public Product(Long sellerId, String name, String description, BigDecimal price,
                   BigDecimal discountPrice, Integer stock, String imageUrl, String category,
                   Boolean featured) {
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.category = category;
        this.featured = featured != null ? featured : false;
    }

    public void update(String name, String description, BigDecimal price,
                       Integer stock, String imageUrl, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void updateDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public void updateFeatured(Boolean featured) {
        this.featured = featured;
    }
}
