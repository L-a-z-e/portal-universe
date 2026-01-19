package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexInitializationService {

    private static final String INDEX_NAME = "products";
    private static final String MAPPING_FILE = "elasticsearch/products-mapping.json";

    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void initializeIndices() {
        try {
            if (!indexExists(INDEX_NAME)) {
                createIndex(INDEX_NAME, MAPPING_FILE);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch indices", e);
        }
    }

    private boolean indexExists(String indexName) throws IOException {
        return esClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
    }

    private void createIndex(String indexName, String mappingFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(mappingFile);

        try (InputStream is = resource.getInputStream()) {
            JsonNode mapping = objectMapper.readTree(is);

            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .withJson(new java.io.StringReader(mapping.toString()))
            ));

            log.info("Created Elasticsearch index: {}", indexName);
        }
    }
}
