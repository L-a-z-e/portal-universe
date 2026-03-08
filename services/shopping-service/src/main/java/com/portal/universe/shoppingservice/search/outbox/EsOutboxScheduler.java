package com.portal.universe.shoppingservice.search.outbox;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsOutboxScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final EsOutboxEventRepository esOutboxEventRepository;
    private final ElasticsearchClient esClient;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void pollAndIndex() {
        List<EsOutboxEvent> pending = esOutboxEventRepository.findPendingForUpdate(BATCH_SIZE);
        if (pending.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (EsOutboxEvent event : pending) {
                switch (event.getActionType()) {
                    case INDEX -> br.operations(op -> op
                            .index(i -> i
                                    .index(event.getIndexName())
                                    .id(event.getDocumentId())
                                    .document(JsonData.fromJson(event.getPayload()))
                            ));
                    case DELETE -> br.operations(op -> op
                            .delete(d -> d
                                    .index(event.getIndexName())
                                    .id(event.getDocumentId())
                            ));
                }
            }

            BulkResponse response = esClient.bulk(br.build());

            for (int i = 0; i < response.items().size(); i++) {
                BulkResponseItem item = response.items().get(i);
                EsOutboxEvent event = pending.get(i);

                if (item.error() != null) {
                    handleFailure(event, item.error().reason());
                } else {
                    event.markIndexed();
                }
            }

            long indexed = pending.stream().filter(e -> e.getStatus() == EsOutboxStatus.INDEXED).count();
            if (indexed > 0) {
                log.info("ES bulk indexed: {}/{} succeeded", indexed, pending.size());
            }

        } catch (Exception e) {
            log.error("ES bulk request failed, all {} events will retry", pending.size(), e);
            pending.forEach(event -> handleFailure(event, e.getMessage()));
        }
    }

    private void handleFailure(EsOutboxEvent event, String reason) {
        event.incrementRetry();
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.markFailed();
            log.error("ES outbox permanently failed: id={}, index={}, docId={}, reason={}",
                    event.getId(), event.getIndexName(), event.getDocumentId(), reason);
        } else {
            log.warn("ES outbox failed (attempt {}/{}): id={}, index={}, docId={}",
                    event.getRetryCount(), MAX_RETRIES, event.getId(),
                    event.getIndexName(), event.getDocumentId());
        }
    }
}