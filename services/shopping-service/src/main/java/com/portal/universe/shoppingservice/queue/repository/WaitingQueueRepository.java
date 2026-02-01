package com.portal.universe.shoppingservice.queue.repository;

import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WaitingQueueRepository
 */
@Repository
public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {

    Optional<WaitingQueue> findByEventTypeAndEventId(String eventType, Long eventId);

    Optional<WaitingQueue> findByEventTypeAndEventIdAndIsActiveTrue(String eventType, Long eventId);

    List<WaitingQueue> findByIsActiveTrue();
}
