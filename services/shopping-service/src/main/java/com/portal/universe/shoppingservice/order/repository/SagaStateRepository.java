package com.portal.universe.shoppingservice.order.repository;

import com.portal.universe.shoppingservice.order.saga.SagaState;
import com.portal.universe.shoppingservice.order.saga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Saga 상태 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {

    /**
     * Saga ID로 상태를 조회합니다.
     *
     * @param sagaId Saga ID
     * @return Saga 상태
     */
    Optional<SagaState> findBySagaId(String sagaId);

    /**
     * 주문 ID로 Saga 상태를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return Saga 상태
     */
    Optional<SagaState> findByOrderId(Long orderId);

    /**
     * 주문 번호로 Saga 상태를 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @return Saga 상태
     */
    Optional<SagaState> findByOrderNumber(String orderNumber);

    /**
     * 특정 상태의 Saga 목록을 조회합니다.
     *
     * @param status Saga 상태
     * @return Saga 상태 목록
     */
    List<SagaState> findByStatus(SagaStatus status);

    /**
     * 보상 실패한 Saga 목록을 조회합니다 (수동 개입 필요).
     *
     * @return 보상 실패한 Saga 목록
     */
    List<SagaState> findByStatusOrderByStartedAtAsc(SagaStatus status);
}
