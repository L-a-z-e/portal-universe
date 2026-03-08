package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.search.document.ProductDocument;
import com.portal.universe.shoppingservice.search.dto.ProductSearchRequest;
import com.portal.universe.shoppingservice.search.dto.ProductSearchResult;
import com.portal.universe.commonlibrary.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final ProductRepository productRepository;

    public PageResponse<ProductSearchResult> search(ProductSearchRequest request) {
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
            return PageResponse.of(results, request.getPage(), request.getSize(), totalHits);

        } catch (Exception e) {
            log.warn("ES search failed, falling back to DB search for keyword: {}", request.getKeyword(), e);
            return searchFromDatabase(request);
        }
    }

    private PageResponse<ProductSearchResult> searchFromDatabase(ProductSearchRequest request) {
        String keyword = request.getKeyword();
        if (!StringUtils.hasText(keyword)) {
            return PageResponse.of(List.of(), request.getPage(), request.getSize(), 0);
        }

        Page<Product> page = productRepository.searchByKeyword(
                keyword, PageRequest.of(request.getPage(), request.getSize()));

        List<ProductSearchResult> results = page.getContent().stream()
                .map(p -> ProductSearchResult.builder()
                        .id(p.getId())
                        .sellerId(p.getSellerId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .price(p.getPrice())
                        .discountPrice(p.getDiscountPrice())
                        .imageUrl(p.getImageUrl())
                        .category(p.getCategory())
                        .featured(p.getFeatured())
                        .score(1.0)
                        .build())
                .toList();

        return PageResponse.of(results, request.getPage(), request.getSize(), page.getTotalElements());
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

            // Price range filter (ES 8.18.x API)
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                b.filter(f -> f
                        .range(r -> r
                                .number(n -> {
                                    n.field("price");
                                    if (request.getMinPrice() != null) {
                                        n.gte(request.getMinPrice().doubleValue());
                                    }
                                    if (request.getMaxPrice() != null) {
                                        n.lte(request.getMaxPrice().doubleValue());
                                    }
                                    return n;
                                })
                        )
                );
            }

            // Category filter — keyword 타입이므로 term(정확 매칭)으로 필터
            if (StringUtils.hasText(request.getCategory())) {
                b.filter(f -> f
                        .term(t -> t
                                .field("category")
                                .value(request.getCategory())
                        )
                );
            }

            // Featured filter — boolean 타입이므로 term으로 필터
            if (request.getFeatured() != null) {
                b.filter(f -> f
                        .term(t -> t
                                .field("featured")
                                .value(request.getFeatured())
                        )
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
                .sellerId(doc.getSellerId())
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .discountPrice(doc.getDiscountPrice())
                .imageUrl(doc.getImageUrl())
                .category(doc.getCategory())
                .featured(doc.getFeatured())
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
