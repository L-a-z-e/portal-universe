package com.portal.universe.shoppingservice.timedeal.service;

import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;

import java.util.List;

public interface TimeDealService {

    /**
     * 타임딜 ID로 타임딜을 조회합니다.
     */
    TimeDealResponse getTimeDeal(Long timeDealId);

    /**
     * 현재 진행중인 모든 타임딜을 조회합니다.
     */
    List<TimeDealResponse> getActiveTimeDeals();

    /**
     * 타임딜 상품을 구매합니다.
     */
    TimeDealPurchaseResponse purchaseTimeDeal(String userId, TimeDealPurchaseRequest request);

    /**
     * 사용자의 타임딜 구매 내역을 조회합니다.
     */
    List<TimeDealPurchaseResponse> getUserPurchases(String userId);
}
