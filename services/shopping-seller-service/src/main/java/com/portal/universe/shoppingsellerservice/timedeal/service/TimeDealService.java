package com.portal.universe.shoppingsellerservice.timedeal.service;

import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TimeDealService {
    TimeDealResponse createTimeDeal(Long sellerId, TimeDealCreateRequest request);
    TimeDealResponse getTimeDeal(Long sellerId, Long timeDealId);
    Page<TimeDealResponse> getSellerTimeDeals(Long sellerId, Pageable pageable);
    void cancelTimeDeal(Long sellerId, Long timeDealId);
}
