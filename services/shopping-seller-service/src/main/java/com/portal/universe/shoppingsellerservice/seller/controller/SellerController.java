package com.portal.universe.shoppingsellerservice.seller.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerRegisterRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerUpdateRequest;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @PostMapping("/register")
    public ApiResponse<SellerResponse> register(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellerRegisterRequest request) {
        return ApiResponse.success(sellerService.register(Long.parseLong(userId), request));
    }

    @GetMapping("/me")
    public ApiResponse<SellerResponse> getMyInfo(@AuthenticationPrincipal String userId) {
        return ApiResponse.success(sellerService.getMyInfo(Long.parseLong(userId)));
    }

    @PutMapping("/me")
    public ApiResponse<SellerResponse> update(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellerUpdateRequest request) {
        return ApiResponse.success(sellerService.update(Long.parseLong(userId), request));
    }
}
