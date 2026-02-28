package com.portal.universe.shoppingservice.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.ProductWithReviewsResponse;
import com.portal.universe.shoppingservice.product.service.ProductService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, ProductController.class})
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ProductResponse createProductResponse(Long id) {
        return new ProductResponse(id, "Test Product", "Description",
                BigDecimal.valueOf(10000), null, 100, "http://img.com/1.jpg", "Electronics",
                false, List.of(), null, 0, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("should_returnProductList_when_getAllProducts")
    void should_returnProductList_when_getAllProducts() throws Exception {
        // given
        ProductResponse response = createProductResponse(1L);
        Page<ProductResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 12), 1);
        when(productService.getAllProducts(any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/products").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(1));
    }

    @Test
    @DisplayName("should_returnProduct_when_getProductById")
    void should_returnProduct_when_getProductById() throws Exception {
        // given
        ProductResponse response = createProductResponse(1L);
        when(productService.getProductById(1L)).thenReturn(response);

        // when/then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Product"));
    }

    @Test
    @DisplayName("should_returnProductWithReviews_when_called")
    void should_returnProductWithReviews_when_called() throws Exception {
        // given
        ProductWithReviewsResponse response = new ProductWithReviewsResponse(
                1L, "Test Product", "Description", BigDecimal.valueOf(10000), 100,
                "http://img.com/1.jpg", "Electronics", List.of());
        when(productService.getProductWithReviews(1L)).thenReturn(response);

        // when/then
        mockMvc.perform(get("/products/1/with-reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.reviews").isArray());
    }

    @Test
    @DisplayName("should_returnDefaultPaging_when_noParamsProvided")
    void should_returnDefaultPaging_when_noParamsProvided() throws Exception {
        // given
        Page<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 12), 0);
        when(productService.getAllProducts(any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
