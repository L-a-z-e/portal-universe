package com.portal.universe.shoppingservice.order.repository;

import com.portal.universe.shoppingservice.order.saga.SagaState;
import com.portal.universe.shoppingservice.order.saga.SagaStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {

    Optional<SagaState> findBySagaId(String sagaId);

    Optional<SagaState> findByOrderId(Long orderId);

    Optional<SagaState> findByOrderNumber(String orderNumber);

    List<SagaState> findByStatus(SagaStatus status);

    List<SagaState> findByStatusOrderByStartedAtAsc(SagaStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT s FROM SagaState s WHERE s.status IN :statuses AND s.startedAt < :cutoff ORDER BY s.startedAt ASC")
    List<SagaState> findDeadSagas(@Param("statuses") List<SagaStatus> statuses,
                                  @Param("cutoff") Instant cutoff);
}
