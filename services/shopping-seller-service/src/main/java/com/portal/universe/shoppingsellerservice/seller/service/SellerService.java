package com.portal.universe.shoppingsellerservice.seller.service;

import com.portal.universe.shoppingsellerservice.seller.dto.SellerRegisterRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerUpdateRequest;

public interface SellerService {
    SellerResponse register(Long userId, SellerRegisterRequest request);
    SellerResponse getMyInfo(Long userId);
    SellerResponse update(Long userId, SellerUpdateRequest request);
}
