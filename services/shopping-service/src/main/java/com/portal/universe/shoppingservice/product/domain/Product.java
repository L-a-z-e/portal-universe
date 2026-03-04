package com.portal.universe.shoppingservice.product.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends BaseEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 19, scale = 2)
    private BigDecimal discountPrice;

    private String imageUrl;

    private String category;

    @Column(nullable = false)
    private Boolean featured = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @Builder
    public Product(Long id, Long sellerId, String name, String description, BigDecimal price, BigDecimal discountPrice,
                   String imageUrl, String category, Boolean featured) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.imageUrl = imageUrl;
        this.category = category;
        this.featured = featured != null ? featured : false;
    }

    public void updateDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public void updateFeatured(Boolean featured) {
        this.featured = featured;
    }

    public void updateFromSource(String name, String description, BigDecimal price,
                                  BigDecimal discountPrice, String imageUrl, String category,
                                  Boolean featured) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.imageUrl = imageUrl;
        this.category = category;
        this.featured = featured != null ? featured : false;
    }
}
