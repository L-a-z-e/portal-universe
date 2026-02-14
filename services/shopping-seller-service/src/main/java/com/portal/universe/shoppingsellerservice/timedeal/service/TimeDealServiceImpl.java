package com.portal.universe.shoppingsellerservice.timedeal.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealServiceImpl implements TimeDealService {

    private final TimeDealRepository timeDealRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public TimeDealResponse createTimeDeal(Long sellerId, TimeDealCreateRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new CustomBusinessException(SellerErrorCode.TIMEDEAL_INVALID_PERIOD);
        }

        for (TimeDealCreateRequest.TimeDealProductItem item : request.products()) {
            var product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.TIMEDEAL_PRODUCT_NOT_FOUND));
            if (!product.getSellerId().equals(sellerId)) {
                throw new CustomBusinessException(SellerErrorCode.PRODUCT_NOT_OWNED);
            }
        }

        TimeDeal timeDeal = request.toEntity(sellerId);

        for (TimeDealCreateRequest.TimeDealProductItem item : request.products()) {
            TimeDealProduct tdp = TimeDealProduct.builder()
                    .timeDeal(timeDeal)
                    .productId(item.productId())
                    .dealPrice(item.dealPrice())
                    .dealQuantity(item.dealQuantity())
                    .maxPerUser(item.maxPerUser())
                    .build();
            timeDeal.addProduct(tdp);
        }

        return TimeDealResponse.from(timeDealRepository.save(timeDeal));
    }

    @Override
    public TimeDealResponse getTimeDeal(Long sellerId, Long timeDealId) {
        TimeDeal timeDeal = timeDealRepository.findById(timeDealId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.TIMEDEAL_NOT_FOUND));
        if (!timeDeal.getSellerId().equals(sellerId)) {
            throw new CustomBusinessException(SellerErrorCode.TIMEDEAL_NOT_OWNED);
        }
        return TimeDealResponse.from(timeDeal);
    }

    @Override
    public Page<TimeDealResponse> getSellerTimeDeals(Long sellerId, Pageable pageable) {
        return timeDealRepository.findBySellerId(sellerId, pageable)
                .map(TimeDealResponse::from);
    }

    @Override
    @Transactional
    public void cancelTimeDeal(Long sellerId, Long timeDealId) {
        TimeDeal timeDeal = timeDealRepository.findById(timeDealId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.TIMEDEAL_NOT_FOUND));
        if (!timeDeal.getSellerId().equals(sellerId)) {
            throw new CustomBusinessException(SellerErrorCode.TIMEDEAL_NOT_OWNED);
        }
        if (!timeDeal.isCancellable()) {
            throw new CustomBusinessException(SellerErrorCode.TIMEDEAL_CANNOT_CANCEL);
        }
        timeDeal.cancel();
    }
}
