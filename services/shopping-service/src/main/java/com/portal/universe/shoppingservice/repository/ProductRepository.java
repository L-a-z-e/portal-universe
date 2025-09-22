package com.portal.universe.shoppingservice.repository;

import com.portal.universe.shoppingservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
