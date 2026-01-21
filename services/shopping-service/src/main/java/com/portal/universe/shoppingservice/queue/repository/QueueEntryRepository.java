package com.portal.universe.shoppingservice.queue.repository;

import com.portal.universe.shoppingservice.queue.domain.QueueEntry;
import com.portal.universe.shoppingservice.queue.domain.QueueStatus;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * QueueEntryRepository
 */
@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByEntryToken(String entryToken);

    Optional<QueueEntry> findByQueueAndUserId(WaitingQueue queue, String userId);

    Optional<QueueEntry> findByQueueAndUserIdAndStatus(WaitingQueue queue, String userId, QueueStatus status);

    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.queue = :queue AND e.status = :status")
    Long countByQueueAndStatus(@Param("queue") WaitingQueue queue, @Param("status") QueueStatus status);

    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.queue = :queue AND e.status = 'WAITING' AND e.joinedAt < :joinedAt")
    Long countWaitingBefore(@Param("queue") WaitingQueue queue, @Param("joinedAt") java.time.LocalDateTime joinedAt);

    @Query("SELECT e FROM QueueEntry e WHERE e.queue = :queue AND e.status = 'WAITING' ORDER BY e.joinedAt ASC LIMIT :limit")
    List<QueueEntry> findTopWaiting(@Param("queue") WaitingQueue queue, @Param("limit") int limit);

    List<QueueEntry> findByQueueAndStatusOrderByJoinedAtAsc(WaitingQueue queue, QueueStatus status);
}
