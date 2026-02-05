package com.portal.universe.shoppingservice.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.product.dto.AdminProductRequest;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.StockUpdateRequest;
import com.portal.universe.shoppingservice.product.service.ProductService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProductController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, AdminProductController.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminProductControllerTest {

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
                BigDecimal.valueOf(10000), 100, "http://img.com/1.jpg", "Electronics",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_createProduct_when_validRequest")
    void should_createProduct_when_validRequest() throws Exception {
        // given
        AdminProductRequest request = new AdminProductRequest("New Product", "Desc",
                BigDecimal.valueOf(15000), 50, "http://img.com/2.jpg", "Electronics");
        ProductResponse response = createProductResponse(1L);
        when(productService.createProductAdmin(any(AdminProductRequest.class))).thenReturn(response);

        // when/then
        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_invalidCreateRequest")
    void should_returnBadRequest_when_invalidCreateRequest() throws Exception {
        // when/then
        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should_updateProduct_when_validRequest")
    void should_updateProduct_when_validRequest() throws Exception {
        // given
        AdminProductRequest request = new AdminProductRequest("Updated Product", "Updated Desc",
                BigDecimal.valueOf(20000), 30, "http://img.com/3.jpg", "Electronics");
        ProductResponse response = createProductResponse(1L);
        when(productService.updateProductAdmin(eq(1L), any(AdminProductRequest.class))).thenReturn(response);

        // when/then
        mockMvc.perform(put("/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_deleteProduct_when_called")
    void should_deleteProduct_when_called() throws Exception {
        // given
        doNothing().when(productService).deleteProduct(1L);

        // when/then
        mockMvc.perform(delete("/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("should_updateProductStock_when_validRequest")
    void should_updateProductStock_when_validRequest() throws Exception {
        // given
        StockUpdateRequest request = new StockUpdateRequest(200);
        ProductResponse response = createProductResponse(1L);
        when(productService.updateProductStock(eq(1L), any(StockUpdateRequest.class))).thenReturn(response);

        // when/then
        mockMvc.perform(patch("/admin/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
