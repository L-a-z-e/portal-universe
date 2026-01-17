package com.portal.universe.shoppingservice.cart.repository;

import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 장바구니 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 사용자 ID와 상태로 장바구니를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param status 장바구니 상태
     * @return 장바구니
     */
    Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);

    /**
     * 사용자 ID와 상태로 장바구니를 조회합니다 (항목과 함께 Fetch Join).
     *
     * @param userId 사용자 ID
     * @param status 장바구니 상태
     * @return 장바구니 (항목 포함)
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = :status")
    Optional<Cart> findByUserIdAndStatusWithItems(@Param("userId") String userId, @Param("status") CartStatus status);

    /**
     * 사용자 ID로 활성 장바구니를 조회합니다 (항목과 함께 Fetch Join).
     *
     * @param userId 사용자 ID
     * @return 활성 장바구니 (항목 포함)
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveCartWithItems(@Param("userId") String userId);

    /**
     * 사용자에게 활성 장바구니가 있는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndStatus(String userId, CartStatus status);
}
