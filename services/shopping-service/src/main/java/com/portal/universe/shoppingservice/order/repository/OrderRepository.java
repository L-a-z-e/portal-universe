package com.portal.universe.shoppingservice.order.repository;

import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 주문 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 번호로 주문을 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @return 주문
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 주문 번호로 주문을 조회합니다 (항목과 함께 Fetch Join).
     *
     * @param orderNumber 주문 번호
     * @return 주문 (항목 포함)
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    /**
     * 사용자의 주문 목록을 조회합니다 (최신순 정렬).
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 사용자의 특정 상태 주문 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, OrderStatus status, Pageable pageable);

    /**
     * 주문 번호 존재 여부를 확인합니다.
     *
     * @param orderNumber 주문 번호
     * @return 존재 여부
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * 특정 상태의 주문 목록을 조회합니다 (Admin).
     *
     * @param status 주문 상태
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * 주문 번호 또는 사용자 ID로 주문을 검색합니다 (Admin).
     *
     * @param orderNumber 주문 번호 검색어
     * @param userId 사용자 ID 검색어
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<Order> findByOrderNumberContainingOrUserIdContaining(String orderNumber, String userId, Pageable pageable);
}
