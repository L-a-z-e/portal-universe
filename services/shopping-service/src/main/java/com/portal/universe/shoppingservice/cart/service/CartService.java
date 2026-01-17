package com.portal.universe.shoppingservice.cart.service;

import com.portal.universe.shoppingservice.cart.dto.*;

/**
 * 장바구니 관리 서비스 인터페이스입니다.
 */
public interface CartService {

    /**
     * 사용자의 장바구니를 조회합니다.
     * 활성 장바구니가 없으면 새로 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 장바구니 정보
     */
    CartResponse getCart(String userId);

    /**
     * 장바구니에 상품을 추가합니다.
     *
     * @param userId 사용자 ID
     * @param request 추가할 상품 정보
     * @return 업데이트된 장바구니 정보
     */
    CartResponse addItem(String userId, AddCartItemRequest request);

    /**
     * 장바구니 항목의 수량을 변경합니다.
     *
     * @param userId 사용자 ID
     * @param itemId 항목 ID
     * @param request 변경할 수량 정보
     * @return 업데이트된 장바구니 정보
     */
    CartResponse updateItemQuantity(String userId, Long itemId, UpdateCartItemRequest request);

    /**
     * 장바구니에서 항목을 제거합니다.
     *
     * @param userId 사용자 ID
     * @param itemId 항목 ID
     * @return 업데이트된 장바구니 정보
     */
    CartResponse removeItem(String userId, Long itemId);

    /**
     * 장바구니를 비웁니다.
     *
     * @param userId 사용자 ID
     * @return 빈 장바구니 정보
     */
    CartResponse clearCart(String userId);

    /**
     * 장바구니를 체크아웃합니다 (주문 생성 준비).
     * 체크아웃 후에는 장바구니가 CHECKED_OUT 상태로 변경됩니다.
     *
     * @param userId 사용자 ID
     * @return 체크아웃된 장바구니 정보
     */
    CartResponse checkout(String userId);
}
