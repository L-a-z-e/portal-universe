package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.search.document.ProductDocument;
import com.portal.universe.shoppingservice.search.dto.ProductSearchRequest;
import com.portal.universe.shoppingservice.search.dto.ProductSearchResult;
import com.portal.universe.shoppingservice.support.fixture.ProductFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private io.micrometer.core.instrument.MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @InjectMocks
    private ProductSearchService productSearchService;

    @BeforeEach
    void setUp() {
        productSearchService.initMetrics();
    }

    @Test
    @DisplayName("should_returnSearchResults_when_searchWithKeyword")
    @SuppressWarnings("unchecked")
    void should_returnSearchResults_when_searchWithKeyword() throws IOException {
        // given
        ProductSearchRequest request = ProductSearchRequest.of("laptop", 0, 20);

        ProductDocument doc = ProductDocument.builder()
                .id(1L)
                .name("Laptop")
                .description("A good laptop")
                .price(BigDecimal.valueOf(1500000))
                .build();

        Hit<ProductDocument> hit = mock(Hit.class);
        when(hit.source()).thenReturn(doc);
        when(hit.score()).thenReturn(5.0);
        when(hit.highlight()).thenReturn(Map.of());

        TotalHits totalHits = mock(TotalHits.class);
        when(totalHits.value()).thenReturn(1L);

        HitsMetadata<ProductDocument> hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(List.of(hit));
        when(hitsMetadata.total()).thenReturn(totalHits);

        SearchResponse<ProductDocument> searchResponse = mock(SearchResponse.class);
        when(searchResponse.hits()).thenReturn(hitsMetadata);

        when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(ProductDocument.class)))
                .thenReturn(searchResponse);

        // when
        var result = productSearchService.search(request);

        // then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getItems().get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("should_fallbackToDbSearch_when_esFails")
    @SuppressWarnings("unchecked")
    void should_fallbackToDbSearch_when_esFails() throws IOException {
        // given
        ProductSearchRequest request = ProductSearchRequest.of("laptop", 0, 20);
        when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(ProductDocument.class)))
                .thenThrow(new IOException("Connection refused"));

        Product product = ProductFixture.builder()
                .id(1L).name("Laptop Pro").description("A great laptop")
                .price(BigDecimal.valueOf(1500000)).build();

        when(productRepository.searchByKeyword(eq("laptop"), any()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

        // when
        var result = productSearchService.search(request);

        // then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("Laptop Pro");
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(productRepository).searchByKeyword(eq("laptop"), any());
    }

    @Test
    @DisplayName("should_returnSearchResults_when_searchWithPriceFilter")
    @SuppressWarnings("unchecked")
    void should_returnSearchResults_when_searchWithPriceFilter() throws IOException {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword("phone")
                .minPrice(100000.0)
                .maxPrice(500000.0)
                .page(0)
                .size(20)
                .build();

        HitsMetadata<ProductDocument> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);
        when(totalHits.value()).thenReturn(0L);
        when(hitsMetadata.hits()).thenReturn(List.of());
        when(hitsMetadata.total()).thenReturn(totalHits);

        SearchResponse<ProductDocument> searchResponse = mock(SearchResponse.class);
        when(searchResponse.hits()).thenReturn(hitsMetadata);

        when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(ProductDocument.class)))
                .thenReturn(searchResponse);

        // when
        var result = productSearchService.search(request);

        // then
        assertThat(result.getItems()).isEmpty();
        verify(esClient).search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(ProductDocument.class));
    }
}
