package com.portal.universe.shoppingsellerservice.seller.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerRegisterRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerUpdateRequest;
import com.portal.universe.shoppingsellerservice.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;

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
}
