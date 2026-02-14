package com.portal.universe.shoppingsellerservice.queue.repository;

import com.portal.universe.shoppingsellerservice.queue.domain.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {
    Optional<WaitingQueue> findByEventTypeAndEventId(String eventType, Long eventId);
}
