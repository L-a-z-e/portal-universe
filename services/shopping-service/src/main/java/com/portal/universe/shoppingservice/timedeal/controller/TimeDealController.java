package com.portal.universe.shoppingservice.timedeal.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.service.TimeDealService;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 타임딜(TimeDeal) 관련 사용자 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/time-deals")
@RequiredArgsConstructor
public class TimeDealController {

    private final TimeDealService timeDealService;

    @GetMapping
    public ApiResponse<List<TimeDealResponse>> getActiveTimeDeals() {
        return ApiResponse.success(timeDealService.getActiveTimeDeals());
    }

    @GetMapping("/{timeDealId}")
    public ApiResponse<TimeDealResponse> getTimeDeal(@PathVariable Long timeDealId) {
        return ApiResponse.success(timeDealService.getTimeDeal(timeDealId));
    }

    @PostMapping("/purchase")
    public ApiResponse<TimeDealPurchaseResponse> purchaseTimeDeal(
            @Valid @RequestBody TimeDealPurchaseRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(timeDealService.purchaseTimeDeal(user.uuid(), request));
    }

    @GetMapping("/my/purchases")
    public ApiResponse<List<TimeDealPurchaseResponse>> getMyPurchases(@CurrentUser AuthUser user) {
        return ApiResponse.success(timeDealService.getUserPurchases(user.uuid()));
    }
}
