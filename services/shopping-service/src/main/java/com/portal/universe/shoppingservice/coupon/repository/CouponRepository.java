package com.portal.universe.shoppingservice.coupon.repository;

import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    List<Coupon> findByStatus(CouponStatus status);

    @Query("SELECT c FROM Coupon c WHERE c.status = :status " +
           "AND c.startsAt <= :now AND c.expiresAt > :now " +
           "AND c.issuedQuantity < c.totalQuantity " +
           "ORDER BY c.expiresAt ASC")
    List<Coupon> findAvailableCoupons(@Param("status") CouponStatus status,
                                       @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' " +
           "AND c.expiresAt <= :now")
    List<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now);
}
