package com.portal.universe.shoppingservice.timedeal.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealPurchase;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealProductRepository;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealPurchaseRepository;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealServiceImpl implements TimeDealService {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealProductRepository timeDealProductRepository;
    private final TimeDealPurchaseRepository timeDealPurchaseRepository;
    private final ProductRepository productRepository;
    private final TimeDealRedisService timeDealRedisService;

    @Override
    @Transactional
    public TimeDealResponse createTimeDeal(TimeDealCreateRequest request) {
        validateTimeDealPeriod(request.startsAt(), request.endsAt());

        TimeDeal timeDeal = TimeDeal.builder()
                .name(request.name())
                .description(request.description())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();

        for (TimeDealCreateRequest.TimeDealProductRequest productRequest : request.products()) {
            Product product = productRepository.findById(productRequest.productId())
                    .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

            TimeDealProduct timeDealProduct = TimeDealProduct.builder()
                    .product(product)
                    .dealPrice(productRequest.dealPrice())
                    .dealQuantity(productRequest.dealQuantity())
                    .maxPerUser(productRequest.maxPerUser())
                    .build();

            timeDeal.addProduct(timeDealProduct);
        }

        TimeDeal savedTimeDeal = timeDealRepository.save(timeDeal);

        log.info("Created time deal: id={}, name={}, products={}",
                savedTimeDeal.getId(), savedTimeDeal.getName(), savedTimeDeal.getProducts().size());

        return TimeDealResponse.from(savedTimeDeal);
    }

    private void validateTimeDealPeriod(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt.isAfter(endsAt)) {
            throw new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_INVALID_PERIOD);
        }
    }

    @Override
    public TimeDealResponse getTimeDeal(Long timeDealId) {
        TimeDeal timeDeal = timeDealRepository.findByIdWithProducts(timeDealId);
        if (timeDeal == null) {
            throw new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_NOT_FOUND);
        }
        return TimeDealResponse.from(timeDeal);
    }

    @Override
    public List<TimeDealResponse> getActiveTimeDeals() {
        LocalDateTime now = LocalDateTime.now();
        return timeDealRepository.findActiveDeals(TimeDealStatus.ACTIVE, now)
                .stream()
                .map(TimeDealResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TimeDealPurchaseResponse purchaseTimeDeal(String userId, TimeDealPurchaseRequest request) {
        TimeDealProduct timeDealProduct = timeDealProductRepository
                .findByIdWithProductAndDeal(request.timeDealProductId())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_PRODUCT_NOT_FOUND));

        TimeDeal timeDeal = timeDealProduct.getTimeDeal();

        validateTimeDealForPurchase(timeDeal);

        // Lua Script를 통한 원자적 구매 처리
        Long result = timeDealRedisService.purchaseProduct(
                timeDeal.getId(),
                timeDealProduct.getProduct().getId(),
                userId,
                request.quantity(),
                timeDealProduct.getMaxPerUser()
        );

        if (result == -1) {
            throw new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_PURCHASE_LIMIT_EXCEEDED);
        }
        if (result == 0) {
            throw new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_SOLD_OUT);
        }

        // DB에 구매 기록 저장
        TimeDealPurchase purchase = TimeDealPurchase.builder()
                .userId(userId)
                .timeDealProduct(timeDealProduct)
                .quantity(request.quantity())
                .purchasePrice(timeDealProduct.getDealPrice())
                .build();

        TimeDealPurchase savedPurchase = timeDealPurchaseRepository.save(purchase);

        // 판매 수량 업데이트
        timeDealProduct.incrementSoldQuantity(request.quantity());
        timeDealProductRepository.save(timeDealProduct);

        log.info("TimeDeal purchase completed: userId={}, dealId={}, productId={}, quantity={}",
                userId, timeDeal.getId(), timeDealProduct.getProduct().getId(), request.quantity());

        return TimeDealPurchaseResponse.from(savedPurchase);
    }

    private void validateTimeDealForPurchase(TimeDeal timeDeal) {
        if (timeDeal.getStatus() != TimeDealStatus.ACTIVE) {
            throw new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_NOT_ACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(timeDeal.getStartsAt()) || now.isAfter(timeDeal.getEndsAt())) {
            throw new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_EXPIRED);
        }
    }

    @Override
    public List<TimeDealPurchaseResponse> getUserPurchases(String userId) {
        return timeDealPurchaseRepository.findByUserIdWithProduct(userId)
                .stream()
                .map(TimeDealPurchaseResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void cancelTimeDeal(Long timeDealId) {
        TimeDeal timeDeal = timeDealRepository.findById(timeDealId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.TIMEDEAL_NOT_FOUND));

        timeDeal.cancel();
        timeDealRepository.save(timeDeal);

        // Redis 캐시 정리
        timeDeal.getProducts().forEach(product ->
                timeDealRedisService.deleteTimeDealCache(
                        timeDeal.getId(),
                        product.getProduct().getId()
                )
        );

        log.info("Cancelled time deal: id={}", timeDealId);
    }
}
