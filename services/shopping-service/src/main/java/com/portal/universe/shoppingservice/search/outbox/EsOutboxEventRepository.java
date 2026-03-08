package com.portal.universe.shoppingservice.search.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EsOutboxEventRepository extends JpaRepository<EsOutboxEvent, Long> {

    @Query(value = """
            SELECT * FROM es_outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EsOutboxEvent> findPendingForUpdate(@Param("batchSize") int batchSize);
}