package com.portal.universe.shoppingsellerservice.seller.repository;

import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
