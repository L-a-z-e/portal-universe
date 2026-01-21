package com.portal.universe.shoppingservice.coupon.repository;

import com.portal.universe.shoppingservice.coupon.domain.UserCoupon;
import com.portal.universe.shoppingservice.coupon.domain.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByUserIdAndCouponId(String userId, Long couponId);

    Optional<UserCoupon> findByUserIdAndCouponId(String userId, Long couponId);

    List<UserCoupon> findByUserId(String userId);

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon " +
           "WHERE uc.userId = :userId AND uc.status = :status")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") String userId,
                                            @Param("status") UserCouponStatus status);

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon " +
           "WHERE uc.userId = :userId AND uc.status = 'AVAILABLE' " +
           "AND uc.expiresAt > :now")
    List<UserCoupon> findAvailableByUserId(@Param("userId") String userId,
                                            @Param("now") LocalDateTime now);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.status = 'AVAILABLE' " +
           "AND uc.expiresAt <= :now")
    List<UserCoupon> findExpiredUserCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.id = :couponId")
    long countByCouponId(@Param("couponId") Long couponId);
}
