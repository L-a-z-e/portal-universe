package com.portal.universe.shoppingsellerservice.seller.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerApplyRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerRegisterRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerUpdateRequest;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<SellerResponse>> apply(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellerApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sellerService.apply(userId, request)));
    }

    @GetMapping("/my-application")
    public ApiResponse<SellerResponse> getMyApplication(@AuthenticationPrincipal String userId) {
        return ApiResponse.success(sellerService.getMyApplication(userId));
    }

    @PostMapping("/register")
    public ApiResponse<SellerResponse> register(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellerRegisterRequest request) {
        return ApiResponse.success(sellerService.register(userId, request));
    }

    @GetMapping("/me")
    public ApiResponse<SellerResponse> getMyInfo(@AuthenticationPrincipal String userId) {
        return ApiResponse.success(sellerService.getMyInfo(userId));
    }

    @PutMapping("/me")
    public ApiResponse<SellerResponse> update(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellerUpdateRequest request) {
        return ApiResponse.success(sellerService.update(userId, request));
    }
}
