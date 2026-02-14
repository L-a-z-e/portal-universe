package com.portal.universe.shoppingsellerservice.coupon.repository;

import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Page<Coupon> findBySellerId(Long sellerId, Pageable pageable);
    boolean existsByCode(String code);
    long countBySellerId(Long sellerId);
    long countBySellerIdAndStatus(Long sellerId, CouponStatus status);
}
