package com.portal.universe.shoppingservice.product.repository;

import com.portal.universe.shoppingservice.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Product 엔티티에 대한 데이터 접근을 처리하는 Spring Data JPA 리포지토리입니다.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 상품명으로 중복 체크
     * @param name 상품명
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /**
     * 상품명으로 상품 조회
     * @param name 상품명
     * @return 상품 정보
     */
    Optional<Product> findByName(String name);

    Page<Product> findByCategory(String category, Pageable pageable);
}
