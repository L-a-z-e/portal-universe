package com.portal.universe.shoppingsellerservice.seller.service;

import com.portal.universe.shoppingsellerservice.seller.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SellerService {
    SellerResponse register(String userId, SellerRegisterRequest request);
    SellerResponse apply(String userId, SellerApplyRequest request);
    SellerResponse getMyApplication(String userId);
    SellerResponse getMyInfo(String userId);
    SellerResponse update(String userId, SellerUpdateRequest request);
    Page<SellerResponse> getSellersByStatus(String status, Pageable pageable);
    Page<SellerResponse> getAllSellers(Pageable pageable);
    SellerResponse reviewSeller(Long sellerId, SellerReviewRequest request, String reviewerId);
}
