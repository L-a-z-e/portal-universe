package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.seller.SellerApplicationRequest;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationResponse;
import com.portal.universe.authservice.auth.service.SellerApplicationService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 셀러 신청 사용자 셀프서비스 API 컨트롤러
 * 인증된 사용자가 셀러 신청을 제출하고 상태를 확인할 수 있습니다.
 */
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerApplicationService sellerApplicationService;

    /**
     * 셀러 신청서를 제출합니다.
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> apply(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellerApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sellerApplicationService.apply(userId, request)));
    }

    /**
     * 내 셀러 신청 현황을 조회합니다.
     */
    @GetMapping("/application")
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> getMyApplication(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(sellerApplicationService.getMyApplication(userId)));
    }
}
