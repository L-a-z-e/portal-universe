package com.portal.universe.shoppingsellerservice.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingsellerservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockAddRequest;
import com.portal.universe.shoppingsellerservice.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InventoryController")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    private static final String USER_ID = "seller-uuid";

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

    @Nested
    @DisplayName("GET /inventory/{productId}")
    class GetInventory {

        @Test
        @DisplayName("should return inventory for product")
        void should_return_inventory() throws Exception {
            // given
            InventoryResponse response = new InventoryResponse(1L, 1L, 100, 0, 100, Instant.now());
            when(inventoryService.getInventory(1L)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/inventory/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId").value(1))
                    .andExpect(jsonPath("$.data.availableQuantity").value(100));
        }
    }

    @Nested
    @DisplayName("PUT /inventory/{productId}/add")
    class AddStock {

        @Test
        @DisplayName("should add stock and return updated inventory")
        void should_add_stock() throws Exception {
            // given
            StockAddRequest request = new StockAddRequest(50, "Restock");
            InventoryResponse response = new InventoryResponse(1L, 1L, 150, 0, 150, Instant.now());
            when(inventoryService.addStock(eq(1L), any(StockAddRequest.class), eq(USER_ID)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(put("/inventory/1/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.availableQuantity").value(150));
        }
    }

    @Nested
    @DisplayName("POST /inventory/{productId}")
    class InitializeInventory {

        @Test
        @DisplayName("should initialize inventory with default quantity")
        void should_initialize_inventory() throws Exception {
            // given
            InventoryResponse response = new InventoryResponse(1L, 10L, 0, 0, 0, Instant.now());
            when(inventoryService.initializeInventory(10L, 0)).thenReturn(response);

            // when & then
            mockMvc.perform(post("/inventory/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should initialize inventory with specified quantity")
        void should_initialize_with_quantity() throws Exception {
            // given
            InventoryResponse response = new InventoryResponse(1L, 10L, 200, 0, 200, Instant.now());
            when(inventoryService.initializeInventory(10L, 200)).thenReturn(response);

            // when & then
            mockMvc.perform(post("/inventory/10").param("initialQuantity", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.availableQuantity").value(200));
        }
    }
}
