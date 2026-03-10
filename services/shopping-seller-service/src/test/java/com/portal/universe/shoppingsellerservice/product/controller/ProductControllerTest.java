package com.portal.universe.shoppingsellerservice.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductUpdateRequest;
import com.portal.universe.shoppingsellerservice.product.service.ProductService;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private SellerService sellerService;

    private static final String USER_ID = "seller-uuid";
    private static final Long SELLER_ID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_SHOPPING_SELLER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubGetSellerIdFromUser() {
        SellerResponse sellerResponse = new SellerResponse(
                SELLER_ID, USER_ID, "Store", "123", "Rep", "010", "e@t.com",
                "Bank", "111", new BigDecimal("10.00"), "ACTIVE", null, null, null, null, Instant.now()
        );
        when(sellerService.getMyInfo(USER_ID)).thenReturn(sellerResponse);
    }

    @Nested
    @DisplayName("GET /products")
    class GetAllProducts {

        @Test
        @DisplayName("should return all products")
        void should_return_all_products() throws Exception {
            // given
            ProductResponse product = new ProductResponse(
                    1L, 1L, "Test Product", "desc", new BigDecimal("10000"),
                    null, 100, "img.jpg", "ELECTRONICS", false, Instant.now(), Instant.now()
            );
            when(productService.getAllProducts(any())).thenReturn(new PageImpl<>(List.of(product)));

            // when & then
            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Test Product"));
        }
    }

    @Nested
    @DisplayName("GET /products/{productId}")
    class GetProduct {

        @Test
        @DisplayName("should return single product")
        void should_return_product() throws Exception {
            // given
            ProductResponse product = new ProductResponse(
                    1L, 1L, "Test Product", "desc", new BigDecimal("10000"),
                    null, 100, "img.jpg", "ELECTRONICS", false, Instant.now(), Instant.now()
            );
            when(productService.getProduct(1L)).thenReturn(product);

            // when & then
            mockMvc.perform(get("/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Test Product"));
        }
    }

    @Nested
    @DisplayName("POST /products")
    class CreateProduct {

        @Test
        @DisplayName("should create product for authenticated seller")
        void should_create_product() throws Exception {
            // given
            stubGetSellerIdFromUser();
            ProductCreateRequest request = new ProductCreateRequest(
                    "New Product", "desc", new BigDecimal("15000"), null, 50, null, "FASHION", false
            );
            ProductResponse response = new ProductResponse(
                    1L, SELLER_ID, "New Product", "desc", new BigDecimal("15000"),
                    null, 50, null, "FASHION", false, Instant.now(), Instant.now()
            );
            when(productService.createProduct(eq(SELLER_ID), any(ProductCreateRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("New Product"));
        }
    }

    @Nested
    @DisplayName("PUT /products/{productId}")
    class UpdateProduct {

        @Test
        @DisplayName("should update product")
        void should_update_product() throws Exception {
            // given
            stubGetSellerIdFromUser();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "Updated", "new desc", new BigDecimal("20000"), null, 200, null, "CLOTHING", true
            );
            ProductResponse response = new ProductResponse(
                    1L, SELLER_ID, "Updated", "new desc", new BigDecimal("20000"),
                    null, 200, null, "CLOTHING", true, Instant.now(), Instant.now()
            );
            when(productService.updateProduct(eq(SELLER_ID), eq(1L), any(ProductUpdateRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(put("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated"));
        }
    }

    @Nested
    @DisplayName("DELETE /products/{productId}")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product")
        void should_delete_product() throws Exception {
            // given
            stubGetSellerIdFromUser();

            // when & then
            mockMvc.perform(delete("/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(productService).deleteProduct(SELLER_ID, 1L);
        }
    }
}
