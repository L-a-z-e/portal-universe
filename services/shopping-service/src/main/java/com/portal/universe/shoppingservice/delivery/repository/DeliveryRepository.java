package com.portal.universe.shoppingservice.delivery.repository;

import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByTrackingNumber(String trackingNumber);

    @Query("SELECT d FROM Delivery d LEFT JOIN FETCH d.histories WHERE d.trackingNumber = :trackingNumber")
    Optional<Delivery> findByTrackingNumberWithHistories(@Param("trackingNumber") String trackingNumber);

    Optional<Delivery> findByOrderId(Long orderId);

    Optional<Delivery> findByOrderNumber(String orderNumber);

    @Query("SELECT d FROM Delivery d LEFT JOIN FETCH d.histories WHERE d.orderNumber = :orderNumber")
    Optional<Delivery> findByOrderNumberWithHistories(@Param("orderNumber") String orderNumber);

    List<Delivery> findByStatus(DeliveryStatus status);
}
