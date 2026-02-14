package com.portal.universe.shoppingsellerservice.seller.service;

import com.portal.universe.shoppingsellerservice.seller.dto.SellerRegisterRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerUpdateRequest;

public interface SellerService {
    SellerResponse register(String userId, SellerRegisterRequest request);
    SellerResponse getMyInfo(String userId);
    SellerResponse update(String userId, SellerUpdateRequest request);
}
