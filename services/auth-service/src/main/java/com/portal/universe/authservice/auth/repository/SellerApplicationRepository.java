package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.SellerApplication;
import com.portal.universe.authservice.auth.domain.SellerApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    Optional<SellerApplication> findByUserIdAndStatus(String userId, SellerApplicationStatus status);

    boolean existsByUserIdAndStatus(String userId, SellerApplicationStatus status);

    Page<SellerApplication> findByStatus(SellerApplicationStatus status, Pageable pageable);

    Page<SellerApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
