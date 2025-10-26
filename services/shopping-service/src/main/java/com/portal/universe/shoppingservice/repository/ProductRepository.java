package com.portal.universe.shoppingservice.repository;

import com.portal.universe.shoppingservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Product 엔티티에 대한 데이터 접근을 처리하는 Spring Data JPA 리포지토리입니다.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}