package com.portal.universe.shoppingsellerservice.seller.repository;

import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import com.portal.universe.shoppingsellerservice.seller.domain.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByUserId(String userId);
    boolean existsByUserId(String userId);
    Page<Seller> findByStatus(SellerStatus status, Pageable pageable);
    Page<Seller> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
