package com.portal.universe.shoppingsellerservice.dashboard.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingsellerservice.dashboard.dto.DashboardStatsResponse;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final TimeDealRepository timeDealRepository;
    private final SellerService sellerService;

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats(@AuthenticationPrincipal String userId) {
        Long sellerId = sellerService.getMyInfo(userId).id();

        long productCount = productRepository.countBySellerId(sellerId);
        long couponCount = couponRepository.countBySellerId(sellerId);
        long activeCouponCount = couponRepository.countBySellerIdAndStatus(sellerId, CouponStatus.ACTIVE);
        long timeDealCount = timeDealRepository.countBySellerId(sellerId);
        long activeTimeDealCount = timeDealRepository.countBySellerIdAndStatus(sellerId, TimeDealStatus.ACTIVE);

        return ApiResponse.success(new DashboardStatsResponse(
                productCount, couponCount, activeCouponCount, timeDealCount, activeTimeDealCount
        ));
    }
}
