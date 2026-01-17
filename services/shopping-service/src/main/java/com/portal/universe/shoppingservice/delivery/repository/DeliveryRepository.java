package com.portal.universe.shoppingservice.delivery.repository;

import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 배송 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    /**
     * 운송장 번호로 배송을 조회합니다.
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 정보
     */
    Optional<Delivery> findByTrackingNumber(String trackingNumber);

    /**
     * 운송장 번호로 배송을 조회합니다 (이력 포함).
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 정보 (이력 포함)
     */
    @Query("SELECT d FROM Delivery d LEFT JOIN FETCH d.histories WHERE d.trackingNumber = :trackingNumber")
    Optional<Delivery> findByTrackingNumberWithHistories(@Param("trackingNumber") String trackingNumber);

    /**
     * 주문 ID로 배송을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 배송 정보
     */
    Optional<Delivery> findByOrderId(Long orderId);

    /**
     * 주문 번호로 배송을 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @return 배송 정보
     */
    Optional<Delivery> findByOrderNumber(String orderNumber);

    /**
     * 주문 번호로 배송을 조회합니다 (이력 포함).
     *
     * @param orderNumber 주문 번호
     * @return 배송 정보 (이력 포함)
     */
    @Query("SELECT d FROM Delivery d LEFT JOIN FETCH d.histories WHERE d.orderNumber = :orderNumber")
    Optional<Delivery> findByOrderNumberWithHistories(@Param("orderNumber") String orderNumber);

    /**
     * 특정 상태의 배송 목록을 조회합니다.
     *
     * @param status 배송 상태
     * @return 배송 목록
     */
    List<Delivery> findByStatus(DeliveryStatus status);
}
