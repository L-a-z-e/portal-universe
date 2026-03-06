package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.search.document.ProductDocument;
import com.portal.universe.shoppingservice.search.dto.ProductSearchRequest;
import com.portal.universe.shoppingservice.search.dto.ProductSearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private ProductSearchService productSearchService;

    @Test
    @DisplayName("should_indexProduct_when_called")
    @SuppressWarnings("unchecked")
    void should_indexProduct_when_called() throws IOException {
        // given
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Test Product");
        when(product.getDescription()).thenReturn("Description");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(10000));

        IndexResponse indexResponse = mock(IndexResponse.class);
        when(esClient.index(any(java.util.function.Function.class))).thenReturn(indexResponse);

        // when
        productSearchService.indexProduct(product);

        // then
        verify(esClient).index(any(java.util.function.Function.class));
    }

    @Test
    @DisplayName("should_callIndexProduct_when_updateProduct")
    @SuppressWarnings("unchecked")
    void should_callIndexProduct_when_updateProduct() throws IOException {
        // given
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Updated Product");
        when(product.getDescription()).thenReturn("Updated");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(20000));

        IndexResponse indexResponse = mock(IndexResponse.class);
        when(esClient.index(any(java.util.function.Function.class))).thenReturn(indexResponse);

        // when
        productSearchService.updateProduct(product);

        // then
        verify(esClient).index(any(java.util.function.Function.class));
    }

    @Test
    @DisplayName("should_deleteProduct_when_called")
    @SuppressWarnings("unchecked")
    void should_deleteProduct_when_called() throws IOException {
        // given
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(esClient.delete(any(java.util.function.Function.class))).thenReturn(deleteResponse);

        // when
        productSearchService.deleteProduct(1L);

        // then
        verify(esClient).delete(any(java.util.function.Function.class));
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
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getTotalHits()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("should_fallbackToDbSearch_when_esFails")
    @SuppressWarnings("unchecked")
    void should_fallbackToDbSearch_when_esFails() throws IOException {
        // given
        ProductSearchRequest request = ProductSearchRequest.of("laptop", 0, 20);
        when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(ProductDocument.class)))
                .thenThrow(new IOException("Connection refused"));

        Product product = Product.builder()
                .name("Laptop Pro")
                .description("A great laptop")
                .price(BigDecimal.valueOf(1500000))
                .build();
        ReflectionTestUtils.setField(product, "id", 1L);

        when(productRepository.searchByKeyword(eq("laptop"), any()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

        // when
        var result = productSearchService.search(request);

        // then
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).getName()).isEqualTo("Laptop Pro");
        assertThat(result.getTotalHits()).isEqualTo(1L);
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
        assertThat(result.getResults()).isEmpty();
        verify(esClient).search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(ProductDocument.class));
    }
}
