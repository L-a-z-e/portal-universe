package com.portal.universe.shoppingservice.timedeal.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.service.TimeDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 타임딜(TimeDeal) 관련 관리자 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/admin/time-deals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTimeDealController {

    private final TimeDealService timeDealService;

    /**
     * 새로운 타임딜을 생성합니다.
     */
    @PostMapping
    public ApiResponse<TimeDealResponse> createTimeDeal(@Valid @RequestBody TimeDealCreateRequest request) {
        return ApiResponse.success(timeDealService.createTimeDeal(request));
    }

    /**
     * 특정 타임딜 정보를 조회합니다.
     */
    @GetMapping("/{timeDealId}")
    public ApiResponse<TimeDealResponse> getTimeDeal(@PathVariable Long timeDealId) {
        return ApiResponse.success(timeDealService.getTimeDeal(timeDealId));
    }

    /**
     * 타임딜을 취소합니다.
     */
    @DeleteMapping("/{timeDealId}")
    public ApiResponse<Void> cancelTimeDeal(@PathVariable Long timeDealId) {
        timeDealService.cancelTimeDeal(timeDealId);
        return ApiResponse.success(null);
    }
}
