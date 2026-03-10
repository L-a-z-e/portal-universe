package com.portal.universe.shoppingservice.timedeal.service;

import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;

import java.util.List;

public interface TimeDealService {

    TimeDealResponse getTimeDeal(Long timeDealId);

    List<TimeDealResponse> getActiveTimeDeals();

    TimeDealPurchaseResponse purchaseTimeDeal(String userId, TimeDealPurchaseRequest request);

    List<TimeDealPurchaseResponse> getUserPurchases(String userId);
}
