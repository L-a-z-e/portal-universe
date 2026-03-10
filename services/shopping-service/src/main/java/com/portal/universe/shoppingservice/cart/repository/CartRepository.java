package com.portal.universe.shoppingservice.cart.repository;

import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findFirstByUserIdAndStatusOrderByIdDesc(String userId, CartStatus status);

    // 중복 카트가 있을 수 있으므로 List로 반환하고 호출측에서 첫 번째를 사용
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = :status ORDER BY c.id DESC")
    List<Cart> findByUserIdAndStatusWithItems(@Param("userId") String userId, @Param("status") CartStatus status);

    // 중복 카트가 있을 수 있으므로 List로 반환하고 호출측에서 첫 번째를 사용
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = 'ACTIVE' ORDER BY c.id DESC")
    List<Cart> findActiveCartWithItems(@Param("userId") String userId);

    boolean existsByUserIdAndStatus(String userId, CartStatus status);
}
