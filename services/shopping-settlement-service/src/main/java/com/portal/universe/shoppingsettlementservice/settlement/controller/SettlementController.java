package com.portal.universe.shoppingsettlementservice.settlement.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementPeriodResponse;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementResponse;
import com.portal.universe.shoppingsettlementservice.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/periods")
    public ApiResponse<List<SettlementPeriodResponse>> getPeriods(
            @RequestParam(defaultValue = "DAILY") String periodType,
            Pageable pageable) {
        return ApiResponse.success(settlementService.getPeriods(periodType, pageable));
    }

    @GetMapping("/periods/{periodId}")
    public ApiResponse<SettlementPeriodResponse> getPeriod(@PathVariable Long periodId) {
        return ApiResponse.success(settlementService.getPeriod(periodId));
    }

    @GetMapping("/sellers/{sellerId}")
    public ApiResponse<Page<SettlementResponse>> getSellerSettlements(
            @PathVariable Long sellerId, Pageable pageable) {
        return ApiResponse.success(settlementService.getSellerSettlements(sellerId, pageable));
    }

    @PostMapping("/periods/{periodId}/confirm")
    public ApiResponse<Void> confirmPeriod(@PathVariable Long periodId) {
        settlementService.confirmPeriod(periodId);
        return ApiResponse.success(null);
    }

    @PostMapping("/periods/{periodId}/pay")
    public ApiResponse<Void> payPeriod(@PathVariable Long periodId) {
        settlementService.markPeriodPaid(periodId);
        return ApiResponse.success(null);
    }
}
