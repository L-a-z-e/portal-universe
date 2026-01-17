package com.portal.universe.shoppingservice.payment.repository;

import com.portal.universe.shoppingservice.payment.domain.Payment;
import com.portal.universe.shoppingservice.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 결제 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 결제 번호로 결제를 조회합니다.
     *
     * @param paymentNumber 결제 번호
     * @return 결제 정보
     */
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    /**
     * 주문 ID로 결제를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 결제 정보
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * 주문 번호로 결제를 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @return 결제 정보
     */
    Optional<Payment> findByOrderNumber(String orderNumber);

    /**
     * 사용자의 결제 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 결제 목록
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 특정 상태의 결제 목록을 조회합니다.
     *
     * @param status 결제 상태
     * @return 결제 목록
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * PG사 거래 ID로 결제를 조회합니다.
     *
     * @param pgTransactionId PG사 거래 ID
     * @return 결제 정보
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);
}
