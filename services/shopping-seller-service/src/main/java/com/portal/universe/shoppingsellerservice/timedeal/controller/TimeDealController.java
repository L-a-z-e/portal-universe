package com.portal.universe.shoppingsellerservice.timedeal.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingsellerservice.timedeal.service.TimeDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/time-deals")
@RequiredArgsConstructor
public class TimeDealController {

    private final TimeDealService timeDealService;
    private final SellerService sellerService;

    @GetMapping
    public ApiResponse<Page<TimeDealResponse>> getSellerTimeDeals(
            @AuthenticationPrincipal String userId,
            Pageable pageable) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(timeDealService.getSellerTimeDeals(sellerId, pageable));
    }

    @GetMapping("/{timeDealId}")
    public ApiResponse<TimeDealResponse> getTimeDeal(
            @AuthenticationPrincipal String userId,
            @PathVariable Long timeDealId) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(timeDealService.getTimeDeal(sellerId, timeDealId));
    }

    @PostMapping
    public ApiResponse<TimeDealResponse> createTimeDeal(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody TimeDealCreateRequest request) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(timeDealService.createTimeDeal(sellerId, request));
    }

    @DeleteMapping("/{timeDealId}")
    public ApiResponse<Void> cancelTimeDeal(
            @AuthenticationPrincipal String userId,
            @PathVariable Long timeDealId) {
        Long sellerId = getSellerIdFromUser(userId);
        timeDealService.cancelTimeDeal(sellerId, timeDealId);
        return ApiResponse.success(null);
    }

    private Long getSellerIdFromUser(String userId) {
        return sellerService.getMyInfo(userId).id();
    }
}
