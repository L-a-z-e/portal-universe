package com.portal.universe.shoppingsellerservice.seller.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.SellerApprovedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import com.portal.universe.shoppingsellerservice.seller.domain.SellerStatus;
import com.portal.universe.shoppingsellerservice.seller.dto.*;
import com.portal.universe.shoppingsellerservice.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SellerResponse register(String userId, SellerRegisterRequest request) {
        if (sellerRepository.existsByUserId(userId)) {
            throw new CustomBusinessException(SellerErrorCode.SELLER_ALREADY_EXISTS);
        }
        Seller seller = request.toEntity(userId);
        return SellerResponse.from(sellerRepository.save(seller));
    }

    @Override
    @Transactional
    public SellerResponse apply(String userId, SellerApplyRequest request) {
        if (sellerRepository.existsByUserId(userId)) {
            throw new CustomBusinessException(SellerErrorCode.SELLER_ALREADY_EXISTS);
        }
        Seller seller = request.toEntity(userId);
        Seller saved = sellerRepository.save(seller);
        log.info("Seller application submitted: userId={}, businessName={}", userId, request.businessName());
        return SellerResponse.from(saved);
    }

    @Override
    public SellerResponse getMyApplication(String userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.SELLER_APPLICATION_NOT_FOUND));
        return SellerResponse.from(seller);
    }

    @Override
    public SellerResponse getMyInfo(String userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.SELLER_NOT_FOUND));
        return SellerResponse.from(seller);
    }

    @Override
    @Transactional
    public SellerResponse update(String userId, SellerUpdateRequest request) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.SELLER_NOT_FOUND));
        seller.update(request.businessName(), request.phone(), request.email(),
                request.bankName(), request.bankAccount());
        return SellerResponse.from(seller);
    }

    @Override
    public Page<SellerResponse> getSellersByStatus(String status, Pageable pageable) {
        SellerStatus sellerStatus = SellerStatus.valueOf(status);
        return sellerRepository.findByStatus(sellerStatus, pageable)
                .map(SellerResponse::from);
    }

    @Override
    public Page<SellerResponse> getAllSellers(Pageable pageable) {
        return sellerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(SellerResponse::from);
    }

    @Override
    @Transactional
    public SellerResponse reviewSeller(Long sellerId, SellerReviewRequest request, String reviewerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.SELLER_NOT_FOUND));

        if (!seller.isPending()) {
            throw new CustomBusinessException(SellerErrorCode.SELLER_APPLICATION_NOT_PENDING);
        }

        if (Boolean.TRUE.equals(request.approved())) {
            seller.approve(reviewerId, request.reviewComment());

            eventPublisher.publishEvent(SellerApprovedEvent.newBuilder()
                    .setUserId(seller.getUserId())
                    .setSellerId(seller.getId())
                    .setApprovedBy(reviewerId)
                    .setTimestamp(Instant.now())
                    .build());

            log.info("Seller approved: sellerId={}, userId={}, by={}", sellerId, seller.getUserId(), reviewerId);
        } else {
            seller.reject(reviewerId, request.reviewComment());
            log.info("Seller rejected: sellerId={}, userId={}, by={}", sellerId, seller.getUserId(), reviewerId);
        }

        return SellerResponse.from(seller);
    }
}
