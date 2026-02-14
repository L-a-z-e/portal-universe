package com.portal.universe.shoppingsellerservice.product.repository;

import com.portal.universe.shoppingsellerservice.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
    Page<Product> findBySellerIdAndCategory(Long sellerId, String category, Pageable pageable);
}
