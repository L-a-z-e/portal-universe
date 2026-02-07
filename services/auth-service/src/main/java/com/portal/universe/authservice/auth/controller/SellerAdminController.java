package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.seller.SellerApplicationResponse;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationReviewRequest;
import com.portal.universe.authservice.auth.service.SellerApplicationService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 셀러 신청 관리 API 컨트롤러 (SHOPPING_ADMIN, SUPER_ADMIN)
 * 관리자가 셀러 신청을 조회하고 승인/거절할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/admin/seller")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')")
public class SellerAdminController {

    private final SellerApplicationService sellerApplicationService;

    /**
     * 대기 중인 셀러 신청 목록을 조회합니다.
     */
    @GetMapping("/applications/pending")
    public ResponseEntity<ApiResponse<PageResponse<SellerApplicationResponse>>> getPendingApplications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                sellerApplicationService.getPendingApplications(pageable))));
    }

    /**
     * 모든 셀러 신청 목록을 조회합니다.
     */
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageResponse<SellerApplicationResponse>>> getAllApplications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                sellerApplicationService.getAllApplications(pageable))));
    }

    /**
     * 셀러 신청을 심사합니다 (승인 또는 거절).
     */
    @PostMapping("/applications/{applicationId}/review")
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> review(
            @PathVariable Long applicationId,
            @Valid @RequestBody SellerApplicationReviewRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(
                ApiResponse.success(sellerApplicationService.review(applicationId, request, adminId)));
    }
}
