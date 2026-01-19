package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.search.document.ProductDocument;
import com.portal.universe.shoppingservice.search.dto.ProductSearchRequest;
import com.portal.universe.shoppingservice.search.dto.ProductSearchResult;
import com.portal.universe.shoppingservice.search.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final String INDEX_NAME = "products";
    private final ElasticsearchClient esClient;

    public void indexProduct(Product product) {
        try {
            ProductDocument document = ProductDocument.from(product);
            IndexResponse response = esClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(product.getId()))
                    .document(document)
            );
            log.info("Indexed product {}: result={}", product.getId(), response.result());
        } catch (IOException e) {
            log.error("Failed to index product {}", product.getId(), e);
        }
    }

    public void updateProduct(Product product) {
        indexProduct(product);  // Upsert
    }

    public void deleteProduct(Long productId) {
        try {
            esClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(String.valueOf(productId))
            );
            log.info("Deleted product {} from index", productId);
        } catch (IOException e) {
            log.error("Failed to delete product {} from index", productId, e);
        }
    }

    public void updateStock(Long productId, Integer stock) {
        try {
            esClient.update(u -> u
                    .index(INDEX_NAME)
                    .id(String.valueOf(productId))
                    .doc(Map.of("stock", stock)),
                    ProductDocument.class
            );
            log.debug("Updated stock for product {}: {}", productId, stock);
        } catch (IOException e) {
            log.error("Failed to update stock for product {}", productId, e);
        }
    }

    public SearchResponse<ProductSearchResult> search(ProductSearchRequest request) {
        try {
            SearchRequest searchRequest = buildSearchRequest(request);
            var response = esClient.search(searchRequest, ProductDocument.class);

            List<ProductSearchResult> results = new ArrayList<>();
            for (Hit<ProductDocument> hit : response.hits().hits()) {
                ProductDocument doc = hit.source();
                if (doc != null) {
                    ProductSearchResult result = mapToSearchResult(doc, hit);
                    results.add(result);
                }
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
            return SearchResponse.of(results, totalHits, request.getPage(), request.getSize());

        } catch (IOException e) {
            log.error("Search failed for keyword: {}", request.getKeyword(), e);
            return SearchResponse.of(List.of(), 0, request.getPage(), request.getSize());
        }
    }

    private SearchRequest buildSearchRequest(ProductSearchRequest request) {
        return SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q.bool(buildBoolQuery(request)))
                .highlight(h -> h
                        .fields("name", f -> f.preTags("<em>").postTags("</em>"))
                        .fields("description", f -> f.preTags("<em>").postTags("</em>"))
                )
                .from(request.getPage() * request.getSize())
                .size(request.getSize())
                .sort(buildSort(request.getSort()))
        );
    }

    private BoolQuery buildBoolQuery(ProductSearchRequest request) {
        return BoolQuery.of(b -> {
            // Keyword search (multi-match with fuzzy)
            if (StringUtils.hasText(request.getKeyword())) {
                b.must(m -> m
                        .multiMatch(mm -> mm
                                .query(request.getKeyword())
                                .fields("name^3", "description")
                                .fuzziness("AUTO")
                        )
                );
            }

            // Price range filter
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                b.filter(f -> f
                        .range(r -> {
                            r.field("price");
                            if (request.getMinPrice() != null) {
                                r.gte(JsonData.of(request.getMinPrice()));
                            }
                            if (request.getMaxPrice() != null) {
                                r.lte(JsonData.of(request.getMaxPrice()));
                            }
                            return r;
                        })
                );
            }

            return b;
        });
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSort(String sort) {
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = new ArrayList<>();

        if (sort == null) {
            return sortOptions;
        }

        switch (sort) {
            case "price_asc":
                sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(
                        so -> so.field(f -> f.field("price").order(SortOrder.Asc))));
                break;
            case "price_desc":
                sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(
                        so -> so.field(f -> f.field("price").order(SortOrder.Desc))));
                break;
            case "newest":
                sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(
                        so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc))));
                break;
            default:
                // relevance - use default scoring
                break;
        }

        return sortOptions;
    }

    private ProductSearchResult mapToSearchResult(ProductDocument doc, Hit<ProductDocument> hit) {
        ProductSearchResult result = ProductSearchResult.builder()
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .stock(doc.getStock())
                .score(hit.score())
                .build();

        // Set highlights if available
        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
            Map<String, String> highlights = new HashMap<>();
            hit.highlight().forEach((field, values) -> {
                if (!values.isEmpty()) {
                    highlights.put(field, values.get(0));
                }
            });
            result.setHighlights(highlights);
        }

        return result;
    }
}
