package com.portal.universe.shoppingservice.timedeal.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.service.TimeDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 타임딜(TimeDeal) 관련 관리자 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/admin/time-deals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminTimeDealController {

    private final TimeDealService timeDealService;

    /**
     * 전체 타임딜 목록을 페이징 조회합니다.
     */
    @GetMapping
    public ApiResponse<Page<TimeDealResponse>> getTimeDeals(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.success(timeDealService.getAllTimeDeals(pageable));
    }

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
