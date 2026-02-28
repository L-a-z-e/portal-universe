package com.portal.universe.shoppingsellerservice.seller.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerReviewRequest;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')")
public class SellerAdminController {

    private final SellerService sellerService;

    @GetMapping
    public ApiResponse<PageResponse<SellerResponse>> getSellers(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ApiResponse.success(PageResponse.from(
                    sellerService.getSellersByStatus(status, pageable)));
        }
        return ApiResponse.success(PageResponse.from(
                sellerService.getAllSellers(pageable)));
    }

    @PostMapping("/{sellerId}/review")
    public ApiResponse<SellerResponse> review(
            @PathVariable Long sellerId,
            @Valid @RequestBody SellerReviewRequest request,
            @AuthenticationPrincipal String adminId) {
        return ApiResponse.success(sellerService.reviewSeller(sellerId, request, adminId));
    }
}
