package com.portal.universe.shoppingsellerservice.queue.repository;

import com.portal.universe.shoppingsellerservice.queue.domain.QueueEntry;
import com.portal.universe.shoppingsellerservice.queue.domain.QueueEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    long countByQueueIdAndStatus(Long queueId, QueueEntryStatus status);
}
