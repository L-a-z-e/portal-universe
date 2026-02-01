package com.portal.universe.shoppingservice.cart.repository;

import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 사용자 ID와 상태로 장바구니를 조회합니다 (가장 최근 1건).
     */
    Optional<Cart> findFirstByUserIdAndStatusOrderByIdDesc(String userId, CartStatus status);

    /**
     * 사용자 ID와 상태로 장바구니를 조회합니다 (항목과 함께 Fetch Join).
     * 중복 카트가 있을 수 있으므로 List로 반환하고 호출측에서 첫 번째를 사용합니다.
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = :status ORDER BY c.id DESC")
    List<Cart> findByUserIdAndStatusWithItems(@Param("userId") String userId, @Param("status") CartStatus status);

    /**
     * 사용자 ID로 활성 장바구니를 조회합니다 (항목과 함께 Fetch Join).
     * 중복 카트가 있을 수 있으므로 List로 반환하고 호출측에서 첫 번째를 사용합니다.
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = 'ACTIVE' ORDER BY c.id DESC")
    List<Cart> findActiveCartWithItems(@Param("userId") String userId);

    /**
     * 사용자에게 활성 장바구니가 있는지 확인합니다.
     */
    boolean existsByUserIdAndStatus(String userId, CartStatus status);
}
